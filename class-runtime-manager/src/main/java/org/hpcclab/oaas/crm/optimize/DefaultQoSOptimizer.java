package org.hpcclab.oaas.crm.optimize;

import io.fabric8.kubernetes.api.model.Quantity;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.SvcPerformanceMetrics;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoQosRequirement;
import org.hpcclab.oaas.proto.ProtoStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.harmonicMean;
import static org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.mean;

public class DefaultQoSOptimizer implements QosOptimizer {
  private static final Logger logger = LoggerFactory.getLogger(DefaultQoSOptimizer.class);

  final String defaultRequestCpu;
  final String defaultRequestMem;
  final CrtMappingConfig.CrtConfig crtConfig;
  final double thresholdUpper;
  final double thresholdLower;
  final double fnThresholdUpper;
  final double fnThresholdLower;
  public DefaultQoSOptimizer(CrtMappingConfig.CrtConfig crtConfig) {
    this.crtConfig = crtConfig;
    CrtMappingConfig.FnConfig fnConfig = crtConfig.functions();
    defaultRequestCpu = Objects.requireNonNullElse(fnConfig.defaultRequestCpu(), "0.5");
    defaultRequestMem = Objects.requireNonNullElse(fnConfig.defaultRequestMem(), "256Mi");
    Map<String, String> conf = crtConfig.optimizerConf();
    if (conf==null) conf = Map.of();
    thresholdUpper = Double.parseDouble(conf
      .getOrDefault("thresholdUpper", "0.85"));
    thresholdLower = Double.parseDouble(conf
      .getOrDefault("thresholdLower", "0.5"));
    fnThresholdUpper = Double.parseDouble(conf
      .getOrDefault("fnThresholdUpper", "0.85"));
    fnThresholdLower = Double.parseDouble(conf
      .getOrDefault("fnThresholdLower", "0.5"));
  }


  public static int limitChange(int currentValue, int targetValue, int maxChange) {
    int change = targetValue - currentValue;
    if (Math.abs(change) > maxChange) {
      return change > 0 ? currentValue + maxChange:currentValue - maxChange;
    } else {
      return targetValue; // No need to cap change
    }
  }

  private static int getStartReplica(CrtMappingConfig.SvcConfig sa, ProtoQosRequirement qos, int minInstance) {
    float startReplica;
    startReplica = sa.startReplicas() + qos.getThroughput() * sa.startReplicasToTpRatio();
    startReplica = Math.max(minInstance, startReplica);
    startReplica = Math.min(startReplica, sa.maxReplicas());
    return Math.round(startReplica);
  }

  @Override
  public CrDeploymentPlan resolve(DeploymentUnit unit, OprcEnvironment environment) {

    var up = environment.availability().uptimePercentage();
    float targetAvail = unit.getCls().getQos().getAvailability();
    var qos = unit.getCls().getQos();
    int minInstance;
    int minAvail;
    CrDataSpec dataSpec;
    if (targetAvail <= 0) {
      dataSpec = new CrDataSpec(2);
      minInstance = 1;
      minAvail = 1;
    } else {
      var replica = Math.log(1 - targetAvail) / Math.log(1 - up);
      var replicaN = (int) Math.max(2, Math.ceil(replica));
      minInstance = replicaN;
      dataSpec = new CrDataSpec(replicaN);
      minAvail = replicaN;
    }
    CrtMappingConfig.SvcConfig invoker = crtConfig.services()
      .get(OprcComponent.INVOKER.getSvc());
    CrtMappingConfig.SvcConfig sa = crtConfig.services()
      .get(OprcComponent.STORAGE_ADAPTER.getSvc());
    CrInstanceSpec invokerSpec = CrInstanceSpec.builder()
      .minInstance(getStartReplica(invoker, qos, minInstance))
      .maxInstance(invoker.maxReplicas())
      .scaleDownDelay(null)
      .targetConcurrency(-1)
      .requestsCpu(parseCpu(invoker.requestCpu()))
      .requestsMemory(parseMem(invoker.requestMemory()))
      .limitsCpu(parseCpu(invoker.limitCpu()))
      .limitsMemory(parseMem(invoker.limitMemory()))
      .minAvail(minAvail)
      .enableHpa(invoker.enableHpa() && !unit.getCls().getConfig().getDisableHpa())
      .build();
    var minSa = getStartReplica(sa, qos, 0);
    CrInstanceSpec saSpec = CrInstanceSpec.builder()
      .minInstance(minSa)
      .maxInstance(sa.maxReplicas())
      .scaleDownDelay(null)
      .targetConcurrency(-1)
      .requestsCpu(parseCpu(sa.requestCpu()))
      .requestsMemory(parseMem(sa.requestMemory()))
      .limitsCpu(parseCpu(sa.limitCpu()))
      .limitsMemory(parseMem(sa.limitMemory()))
      .minAvail(minAvail)
      .enableHpa(sa.enableHpa() && !unit.getCls().getConfig().getDisableHpa())
      .disable((unit.getCls().getStateSpec().getKeySpecsCount()==0
        && unit.getCls().getStateType()!=ProtoStateType.PROTO_STATE_TYPE_COLLECTION)
        || minSa==0
      )
      .build();
    var instances = Map.of(
      OprcComponent.INVOKER, invokerSpec,
      OprcComponent.STORAGE_ADAPTER, saSpec
    );
    var fnInstances = unit.getFnListList()
      .stream()
      .map(f -> Map.entry(f.getKey(), resolve(f)))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return CrDeploymentPlan.builder()
      .coreInstances(instances)
      .fnInstances(fnInstances)
      .dataSpec(dataSpec)
      .build();
  }

  @Override
  public CrAdjustmentPlan adjust(CrController controller, CrPerformanceMetrics metrics) {
    Map<String, CrInstanceSpec> fnInstance = Maps.mutable.empty();
    var currentPlan = controller.currentPlan();
    for (var entry : controller.getAttachedFn().entrySet()) {
      var fnKey = entry.getKey();
      var fn = entry.getValue();
      var fnMetrics = metrics.fnMetrics().get(fnKey);
      if (fnMetrics==null) continue;
      CrInstanceSpec instanceSpec = currentPlan.fnInstances().get(fnKey);
      var adjust = adjustComponent(
        controller,
        instanceSpec,
        controller.getTemplate().getConfig().functions(),
        fn.getQos(),
        fnMetrics,
        fnKey,
        true
      );
      if (adjust.change()) fnInstance.put(fnKey, adjust.spec());
    }
    Map<OprcComponent, CrInstanceSpec> coreInstance = computeCls(controller, metrics);

    if (currentPlan==null)
      return CrAdjustmentPlan.DEFAULT;
    return new CrAdjustmentPlan(
      coreInstance,
      fnInstance,
      CrDataSpec.DEFAULT,
      !coreInstance.isEmpty() || !fnInstance.isEmpty()
    );
  }

  private AdjustComponent adjustComponent(CrController controller,
                                          CrInstanceSpec instanceSpec,
                                          CrtMappingConfig.ScalingConfig svcConfig,
                                          ProtoQosRequirement qos,
                                          SvcPerformanceMetrics metrics,
                                          String name,
                                          boolean isFunc) {
    if (metrics==null)
      return AdjustComponent.NONE;
    if (svcConfig.disableDynamicAdjustment())
      return AdjustComponent.NONE;
    int targetRps = qos.getThroughput();
    if (targetRps <= 0)
      return AdjustComponent.NONE;
    metrics = metrics.filterByStableTime(controller.getStableTime(name));
    var meanRps = harmonicMean(metrics.rps());
    var meanCpu = mean(metrics.cpu());
    if (meanRps < 1) {// < 1 is too little. Preventing result explode
      return AdjustComponent.NONE; // prevent overriding throughput guarantee
    }
    var totalRequestCpu = Math.max(1, instanceSpec.minInstance()) * instanceSpec.requestsCpu();
    var cpuPerRps = meanCpu / meanRps;
    double expectedCpu = Math.max(0, cpuPerRps * targetRps);
    int expectedInstance = (int) Math.ceil(expectedCpu / instanceSpec.requestsCpu()); // or limit?
    var cpuPercentage = meanCpu / totalRequestCpu;
    var lower = isFunc ? fnThresholdLower:thresholdLower;
    var upper = isFunc ? fnThresholdUpper:thresholdUpper;
    var expectedRps = cpuPercentage < 1 ? meanRps /cpuPercentage : meanRps;
    var prevInstance = instanceSpec.minInstance();
    var nextInstance = prevInstance;
    var objectiveMissThreshold = svcConfig.objectiveMissThreshold() <= 0? 1: svcConfig.objectiveMissThreshold();
    var idleFilterThreshold = svcConfig.idleFilterThreshold() <= 0? 0.2: svcConfig.idleFilterThreshold();
    logger.debug("compute adjust[1] on ({} : {}), meanRps {}, expectedRps {} (>{}), meanCpu {}, cpuPerRps {}, targetRps {}, cpuPercentage {} ({}<{}), expectedInstance {}",
      controller.getTsidString(), name, meanRps, expectedRps,
      targetRps * objectiveMissThreshold, meanCpu, cpuPerRps,
      targetRps, cpuPercentage, lower, upper, expectedInstance);

    /*
    * over provisioning
    ++ low utilization (cpu-util < thresh_low_util)  while meet the objective (real rps > objective)
    * under provisioning
    ++ high utilization (cpu-util > thresh_high_util) while not meet the objective (real rps < objective)
    ++ expect that objective is not fulfilment by current resource ( expect_rps / objective < thresh_objective_fulfilment) with the current utilization higher than threshold
     */
    if (cpuPercentage < lower && meanRps > targetRps) {
      nextInstance = Math.min(expectedInstance, nextInstance);
    } else if (cpuPercentage > upper && meanRps < targetRps) {
      nextInstance = Math.max(expectedInstance, nextInstance);
    } else if (expectedRps / targetRps < objectiveMissThreshold
      && cpuPercentage > idleFilterThreshold) {
      nextInstance = Math.max(expectedInstance, nextInstance);
    } else {
      return AdjustComponent.NONE;
    }

    int capChanged = limitChange(instanceSpec.minInstance(), nextInstance, svcConfig.maxScaleStep());
    capChanged = Math.max(capChanged, instanceSpec.minAvail());
    if (instanceSpec.maxInstance() > 0) {
      capChanged = Math.min(capChanged, instanceSpec.maxInstance());
    }
    var adjust = instanceSpec.toBuilder().minInstance(capChanged).build();
    var needChange = !instanceSpec.equals(adjust);
    logger.debug("compute adjust[2] on ({} : {}), nextInstance {}, prevInstance {}, maxInstance {}, capChanged {}, needChange {}",
      controller.getTsidString(), name, nextInstance, prevInstance, instanceSpec.maxInstance(), capChanged, needChange);

    if (needChange)
      logger.debug("next adjustment ({} : {}) : {}", controller.getTsidString(), name, adjust);
    return new AdjustComponent(
      needChange,
      adjust
    );
  }

  private CrInstanceSpec resolve(ProtoOFunction fn) {
    CrtMappingConfig.FnConfig fnConfig = crtConfig.functions();
    var provision = fn.getProvision();
    var kn = provision.getKnative();
    int minScale = kn.getMinScale();
    if (minScale < 0)
      minScale = Math.max(0, fnConfig.startReplicas());
    if (provision.getDeployment().getReplicas() > 0) {
      minScale = provision.getDeployment().getReplicas();
    }
    float requestedCpu = parseCpu(kn.getRequestsCpu().isEmpty() ? defaultRequestCpu:kn.getRequestsCpu());
    long requestsMemory = parseMem(kn.getRequestsMemory().isEmpty() ? defaultRequestMem:kn.getRequestsMemory());
    return CrInstanceSpec.builder()
      .minInstance(minScale)
      .maxInstance(kn.getMaxScale())
      .scaleDownDelay(kn.getScaleDownDelay())
      .targetConcurrency(kn.getTargetConcurrency())
      .requestsCpu(requestedCpu)
      .requestsMemory(requestsMemory)
      .limitsCpu(parseCpu(kn.getLimitsCpu()))
      .limitsMemory(parseMem(kn.getLimitsMemory()))
      .minAvail(minScale)
      .build();
  }

  float parseCpu(String val) {
    if (val==null || val.isEmpty())
      return -1;
    return Quantity.parse(val).getNumericalAmount().floatValue();
  }

  long parseMem(String val) {
    if (val==null || val.isEmpty())
      return -1;
    return Quantity.parse(val).getNumericalAmount().longValue();
  }

  private Map<OprcComponent, CrInstanceSpec> computeCls(CrController controller,
                                                        CrPerformanceMetrics metrics) {
    Map<OprcComponent, CrInstanceSpec> adjustPlanMap = Maps.mutable.empty();
    var cls = controller.getAttachedCls().values().iterator().next();
    CrDeploymentPlan currentPlan = controller.currentPlan();
    CrInstanceSpec instanceSpec = currentPlan.coreInstances().get(OprcComponent.INVOKER);
    var adjust = adjustComponent(
      controller,
      instanceSpec,
      controller.getTemplate().getConfig().services().get(OprcComponent.INVOKER.getSvc()),
      cls.getQos(),
      metrics.coreMetrics().get(OprcComponent.INVOKER),
      OprcComponent.INVOKER.name(),
      false);
    if (adjust.change)
      adjustPlanMap.put(OprcComponent.INVOKER, adjust.spec);
    return adjustPlanMap;
  }

  public record AdjustComponent(boolean change, CrInstanceSpec spec) {
    static final AdjustComponent NONE = new AdjustComponent(false, null);
  }
}
