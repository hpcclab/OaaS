package org.hpcclab.oaas.crm.optimize;

import io.fabric8.kubernetes.api.model.Quantity;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.CrComponent;
import org.hpcclab.oaas.crm.CrtMappingConfig;
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

public abstract class AbstractQoSOptimizer implements QosOptimizer {
  private static final Logger logger = LoggerFactory.getLogger(AbstractQoSOptimizer.class);

  final String defaultRequestCpu;
  final String defaultRequestMem;
  final CrtMappingConfig.CrtConfig crtConfig;
  final double thresholdUpper;
  final double thresholdLower;
  final double fnThresholdUpper;
  final double fnThresholdLower;
  final int minDatapoints;

  protected AbstractQoSOptimizer(CrtMappingConfig.CrtConfig crtConfig) {
    this.crtConfig = crtConfig;
    CrtMappingConfig.FnConfig fnConfig = crtConfig.functions();
    defaultRequestCpu = Objects.requireNonNullElse(fnConfig.defaultRequestCpu(), "0.5");
    defaultRequestMem = Objects.requireNonNullElse(fnConfig.defaultRequestMem(), "256Mi");
    Map<String, String> conf = crtConfig.optimizerConf();
    if (conf==null) conf = Map.of();
    thresholdUpper = Double.parseDouble(conf
      .getOrDefault("thresholdUpper", "1"));
    thresholdLower = Double.parseDouble(conf
      .getOrDefault("thresholdLower", "0.4"));
    fnThresholdUpper = Double.parseDouble(conf
      .getOrDefault("fnThresholdUpper", "0.9"));
    fnThresholdLower = Double.parseDouble(conf
      .getOrDefault("fnThresholdLower", "0.6"));
    minDatapoints = Integer.parseInt(conf
      .getOrDefault("minDatapoints", "3"));
  }


  public static int limitChange(int currentValue, int targetValue, int maxChange) {
    int change = targetValue - currentValue;
    if (Math.abs(change) > maxChange) {
      return change > 0 ? currentValue + maxChange:currentValue - maxChange;
    } else {
      return targetValue; // No need to cap change
    }
  }

  public static int getStartReplica(CrtMappingConfig.CrComponentConfig sa, ProtoQosRequirement qos, int minInstance) {
    float startReplica;
    startReplica = sa.startReplicas() + qos.getThroughput() * sa.startReplicasToTpRatio();
    startReplica = Math.max(minInstance, startReplica);
    startReplica = Math.min(startReplica, sa.maxReplicas());
    return Math.round(startReplica);
  }

  @Override
  public CrAdjustmentPlan adjust(CrController controller, CrPerformanceMetrics metrics) {
    Map<String, CrInstanceSpec> fnInstance = Maps.mutable.empty();
    var currentPlan = controller.currentPlan();
    for (var entry : controller.getAttachedFn().entrySet()) {
      var fnKey = entry.getKey();
      var fn = entry.getValue();
      var fnMetrics = metrics.fnMetrics().get(fnKey);
      if (fnMetrics==null) {
        continue;
      }
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
    Map<CrComponent, CrInstanceSpec> coreInstance = computeCls(controller, metrics);

    if (currentPlan==null)
      return CrAdjustmentPlan.DEFAULT;
    return new CrAdjustmentPlan(
      coreInstance,
      fnInstance,
      CrDataSpec.DEFAULT,
      !coreInstance.isEmpty() || !fnInstance.isEmpty()
    );
  }

  protected abstract AdjustComponent adjustComponent(CrController controller,
                                                     CrInstanceSpec instanceSpec,
                                                     CrtMappingConfig.ScalingConfig svcConfig,
                                                     ProtoQosRequirement qos,
                                                     SvcPerformanceMetrics metrics,
                                                     String name,
                                                     boolean isFunc);


  @Override
  public CrDeploymentPlan resolve(DeploymentUnit unit, OprcEnvironment environment) {
    var up = environment.availability().uptimePercentage();
    double targetAvail = unit.getCls().getQos().getAvailability();
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
      minInstance = (int) Math.max(1, Math.ceil(replica));
      var replicaN = Math.max(2, minInstance);
      dataSpec = new CrDataSpec(replicaN);
      minAvail = minInstance;
    }
    CrtMappingConfig.CrComponentConfig invoker = crtConfig.services()
      .get(CrComponent.INVOKER.getSvc());
    CrtMappingConfig.CrComponentConfig sa = crtConfig.services()
      .get(CrComponent.STORAGE_ADAPTER.getSvc());
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
      .enableHpa(invoker.enableHpa())
      .build();

    var instances = Maps.mutable.of(CrComponent.INVOKER, invokerSpec);
    if (sa!=null) {
      var minSa = getStartReplica(sa, qos, 1);
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
        .enableHpa(sa.enableHpa())
        .disable((unit.getCls().getStateSpec().getKeySpecsCount()==0
          && unit.getCls().getStateType()!=ProtoStateType.PROTO_STATE_TYPE_COLLECTION)
          || minSa==0
        )
        .build();
      instances.put(CrComponent.STORAGE_ADAPTER, saSpec);
    }
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

  private CrInstanceSpec resolve(ProtoOFunction fn) {
    CrtMappingConfig.FnConfig fnConfig = crtConfig.functions();
    var provision = fn.getProvision();
    var kn = provision.getKnative();
    var deployment = provision.getDeployment();
    if (!kn.getImage().isEmpty()) {
      int minScale = kn.getMinScale();
      if (minScale < 0)
        minScale = Math.max(0, fnConfig.startReplicas());
      int minAvail = minScale;
      int maxScale = kn.getMaxScale();
      float requestedCpu = parseCpu(kn.getRequestsCpu().isEmpty() ? defaultRequestCpu:kn.getRequestsCpu());
      long requestsMemory = parseMem(kn.getRequestsMemory().isEmpty() ? defaultRequestMem:kn.getRequestsMemory());
      return CrInstanceSpec.builder()
        .minInstance(minScale)
        .maxInstance(maxScale)
        .scaleDownDelay(kn.getScaleDownDelay())
        .targetConcurrency(kn.getTargetConcurrency())
        .requestsCpu(requestedCpu)
        .requestsMemory(requestsMemory)
        .limitsCpu(parseCpu(kn.getLimitsCpu()))
        .limitsMemory(parseMem(kn.getLimitsMemory()))
        .minAvail(minAvail)
        .build();
    } else if (!deployment.getImage().isEmpty()) {
      int minScale = deployment.getMinScale();
      if (minScale <= 0)
        minScale = Math.max(1, fnConfig.startReplicas());
      int minAvail = minScale;
      int maxScale = deployment.getMaxScale();
      if (maxScale <= 0) maxScale = fnConfig.defaultMaxScale();
      float requestedCpu = parseCpu(deployment.getRequestsCpu().isEmpty() ? defaultRequestCpu:deployment.getRequestsCpu());
      long requestsMemory = parseMem(deployment.getRequestsMemory().isEmpty() ? defaultRequestMem:deployment.getRequestsMemory());
      return CrInstanceSpec.builder()
        .minInstance(minScale)
        .maxInstance(maxScale)
        .requestsCpu(requestedCpu)
        .requestsMemory(requestsMemory)
        .limitsCpu(parseCpu(deployment.getLimitsCpu()))
        .limitsMemory(parseMem(deployment.getLimitsMemory()))
        .minAvail(minAvail)
        .build();
    }
    return CrInstanceSpec.builder().build();
  }

  protected float parseCpu(String val) {
    if (val==null || val.isEmpty())
      return -1;
    return Quantity.parse(val).getNumericalAmount().floatValue();
  }

  protected long parseMem(String val) {
    if (val==null || val.isEmpty())
      return -1;
    return Quantity.parse(val).getNumericalAmount().longValue();
  }


  private Map<CrComponent, CrInstanceSpec> computeCls(CrController controller,
                                                      CrPerformanceMetrics metrics) {
    Map<CrComponent, CrInstanceSpec> adjustPlanMap = Maps.mutable.empty();
    var cls = controller.getAttachedCls().values().iterator().next();
    CrDeploymentPlan currentPlan = controller.currentPlan();
    CrInstanceSpec instanceSpec = currentPlan.coreInstances().get(CrComponent.INVOKER);
    var adjust = adjustComponent(
      controller,
      instanceSpec,
      controller.getTemplate().getConfig().services().get(CrComponent.INVOKER.getSvc()),
      cls.getQos(),
      metrics.coreMetrics().get(CrComponent.INVOKER),
      CrComponent.INVOKER.name(),
      false);
    if (adjust.change)
      adjustPlanMap.put(CrComponent.INVOKER, adjust.spec);
    return adjustPlanMap;
  }

  public record AdjustComponent(boolean change, CrInstanceSpec spec) {
    static final AdjustComponent NONE = new AdjustComponent(false, null);
  }
}
