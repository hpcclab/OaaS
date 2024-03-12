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

  public DefaultQoSOptimizer(CrtMappingConfig.CrtConfig crtConfig) {
    this.crtConfig = crtConfig;
    CrtMappingConfig.FnConfig fnConfig = crtConfig.functions();
    defaultRequestCpu = Objects.requireNonNullElse(fnConfig.defaultRequestCpu(), "0.5");
    defaultRequestMem = Objects.requireNonNullElse(fnConfig.defaultRequestMem(), "256Mi");
    Map<String, String> conf = crtConfig.optimizerConf();
    if (conf == null) conf = Map.of();
    thresholdUpper = Double.parseDouble(conf
      .getOrDefault("thresholdUpper", "0.9"));
    thresholdLower = Double.parseDouble(conf
      .getOrDefault("thresholdLower", "0.7"));
  }



  public static int limitChange(int currentValue, int targetValue, int maxChange) {
    int change = targetValue - currentValue;
    if (Math.abs(change) > maxChange) {
      return change > 0 ? currentValue + maxChange : currentValue - maxChange;
    } else {
      return targetValue; // No need to cap change
    }
  }

  @Override
  public CrDeploymentPlan resolve(DeploymentUnit unit, OprcEnvironment environment) {

    var up = environment.availability().uptimePercentage();
    float targetAvail = unit.getCls().getQos().getAvailability();
    int minInstance;
    int minAvail;
    CrDataSpec dataSpec;
    if (targetAvail <= 0) {
      dataSpec = new CrDataSpec(2);
      minInstance = 1;
      minAvail = 0;
    } else {
      var replica = Math.log(1 - targetAvail) / Math.log(1 - up);
      var replicaN = (int) Math.max(2, Math.ceil(replica));
      minInstance = replicaN;
      dataSpec = new CrDataSpec(replicaN);
      minAvail = replicaN;
    }
    CrtMappingConfig.SvcConfig invoker = crtConfig.services().get(OprcComponent.INVOKER.getSvc());
    CrtMappingConfig.SvcConfig sa = crtConfig.services().get(OprcComponent.STORAGE_ADAPTER.getSvc());
    var instances = Map.of(
      OprcComponent.INVOKER, CrInstanceSpec.builder()
        .minInstance(minInstance)
        .maxInstance(-1)
        .scaleDownDelay(null)
        .targetConcurrency(-1)
        .requestsCpu(parseCpu(invoker.requestCpu()))
        .requestsMemory(parseMem(invoker.requestMemory()))
        .limitsCpu(parseCpu(invoker.limitCpu()))
        .limitsMemory(parseMem(invoker.limitMemory()))
        .minAvail(minAvail)
        .build(),
      OprcComponent.STORAGE_ADAPTER, CrInstanceSpec.builder()
        .minInstance(1)
        .maxInstance(-1)
        .scaleDownDelay(null)
        .targetConcurrency(-1)
        .requestsCpu(parseCpu(sa.requestCpu()))
        .requestsMemory(parseMem(sa.requestMemory()))
        .limitsCpu(parseCpu(sa.limitCpu()))
        .limitsMemory(parseMem(sa.limitMemory()))
        .minAvail(minAvail)
        .build()
    );
    var fnInstances = unit.getFnListList()
      .stream()
      .map(f -> Map.entry(f.getKey(), convert(f)))
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
        0
      );
      if (adjust.change()) fnInstance.put(fnKey, adjust.spec());
    }
    Map<OprcComponent, CrInstanceSpec> coreInstance = computeCls(controller, metrics);

    if (currentPlan==null)
      return new CrAdjustmentPlan(Map.of(), Map.of(), false);
    return new CrAdjustmentPlan(
      coreInstance,
      fnInstance,
      !coreInstance.isEmpty() || !fnInstance.isEmpty());
  }

  private AdjustComponent adjustComponent(CrController controller,
                                                 CrInstanceSpec instanceSpec,
                                                 CrtMappingConfig.ScalingConfig svcConfig,
                                                 ProtoQosRequirement qos,
                                                 SvcPerformanceMetrics metrics,
                                                 String name,
                                                 int hardMinInstance) {
    if (metrics==null)
      return AdjustComponent.NONE;
    int targetRps = qos.getThroughput();
    var meanRps = harmonicMean(metrics.rps());
    var meanCpu = mean(metrics.cpu());
    if (targetRps <= 0)
      return AdjustComponent.NONE;
    if (meanRps < 1) {// < 1 is too little. Preventing result explode
      return AdjustComponent.NONE; // prevent overriding throughput guarantee
    }
    var totalRequestCpu = instanceSpec.minInstance() * instanceSpec.requestsCpu();
    var cpuPerRps = meanCpu / meanRps;
    double expectedCpu = Math.max(0, cpuPerRps * targetRps);
    int expectedInstance = (int) Math.ceil(expectedCpu / instanceSpec.requestsCpu()); // or limit?
    if (expectedInstance < hardMinInstance) expectedInstance = hardMinInstance;
    var cpuPercentage = meanCpu / totalRequestCpu;
    var nextInstance = instanceSpec.minInstance();

    if ((meanRps / targetRps > cpuPercentage && cpuPercentage < thresholdLower)
        || (meanRps / targetRps < cpuPercentage && cpuPercentage > thresholdUpper)) {
      nextInstance = expectedInstance;
    }

    int capChanged = limitChange(instanceSpec.minInstance(), nextInstance, svcConfig.maxScaleDiff());
    capChanged = Math.max(capChanged, instanceSpec.minAvail());
    var adjust = instanceSpec.toBuilder().minInstance(capChanged).build();
    logger.debug("compute adjust on {} : {} : meanRps {}, meanCpu {}, cpuPerRps {}, targetRps {}, expectedInstance {}, nextInstance {}, capChanged {}",
      controller.getId(), name, meanRps, meanCpu, cpuPerRps, targetRps, expectedInstance, nextInstance, capChanged);
    var changed = !instanceSpec.equals(adjust);
    logger.debug("got adjustment {} : {} : ({}) {}",
      controller.getId(), name, changed, adjust);
    return new AdjustComponent(
      changed,
      adjust
    );
  }

  private CrInstanceSpec convert(ProtoOFunction fn) {
    var provision = fn.getProvision();
    var qos = fn.getQos();
    var kn = provision.getKnative();
    int minScale = kn.getMinScale();
    if (minScale < 0) minScale = qos.getThroughput() > 0 ? 1:0;
    float requestedCpu = parseCpu(kn.getRequestsCpu().isEmpty() ? defaultRequestCpu:kn.getRequestsCpu());
    long requestsMemory = parseMem(kn.getRequestsMemory().isEmpty() ? defaultRequestMem:kn.getRequestsMemory());
    return CrInstanceSpec.builder()
      .maxInstance(minScale)
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
      1);
    if (adjust.change)
      adjustPlanMap.put(OprcComponent.INVOKER, adjust.spec);
    return adjustPlanMap;
  }

  public record AdjustComponent(boolean change, CrInstanceSpec spec) {
    static final AdjustComponent NONE = new AdjustComponent(false, null);
  }
}
