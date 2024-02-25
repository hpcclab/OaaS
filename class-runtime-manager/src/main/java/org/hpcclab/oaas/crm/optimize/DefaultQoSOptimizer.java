package org.hpcclab.oaas.crm.optimize;

import io.fabric8.kubernetes.api.model.Quantity;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.SvcPerformanceMetrics;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoQosRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.harmonicMean;
import static org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.mean;

public class DefaultQoSOptimizer implements QosOptimizer {
  private static final Logger logger = LoggerFactory.getLogger(DefaultQoSOptimizer.class);

  final String defaultRequestCpu;
  final String defaultRequestMem;

  public DefaultQoSOptimizer(CrtMappingConfig.CrtConfig crtConfig) {
    Map<String, String> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    if (crtConfig.optimizerConf()!=null)
      treeMap.putAll(crtConfig.optimizerConf());
    defaultRequestCpu = treeMap.getOrDefault("defaultRequestCpu", "0.5");
    defaultRequestMem = treeMap.getOrDefault("defaultRequestMem", "256Mi");
  }

  @Override
  public CrDeploymentPlan resolve(DeploymentUnit unit) {
    var instances = Map.of(
      OprcComponent.LOADBALANCER, new CrInstanceSpec(
        1, -1,
        null,
        -1,
        0.5f,
        256L * 1024 * 1024,
        2f,
        1024L * 1024 * 1024
      ),
      OprcComponent.INVOKER, new CrInstanceSpec(
        1, -1,
        null,
        -1,
        0.5f,
        512L * 1024 * 1024,
        2f,
        2048L * 1024 * 1024
      ),
      OprcComponent.STORAGE_ADAPTER, new CrInstanceSpec(
        1, -1,
        null,
        -1,
        0.2f,
        256L * 1024 * 1024,
        2f,
        1024L * 1024 * 1024
      )
    );
    var fnInstances = unit.getFnListList()
      .stream()
      .map(f -> Map.entry(f.getKey(), convert(f)))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new CrDeploymentPlan(
      instances, fnInstances
    );
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
      var adj = computeFunc(controller, fn, fnMetrics);
      if (adj.change()) fnInstance.put(fnKey, adj.spec());
    }
    Map<OprcComponent, CrInstanceSpec> coreInstance = computeCls(controller, metrics);

    if (currentPlan==null)
      return new CrAdjustmentPlan(Map.of(), Map.of(), false);
    return new CrAdjustmentPlan(
      coreInstance,
      fnInstance,
      !coreInstance.isEmpty() || !fnInstance.isEmpty());
  }


  CrInstanceSpec convert(ProtoOFunction fn) {
    var provision = fn.getProvision();
    var qos = fn.getQos();
    var kn = provision.getKnative();
    int minScale = kn.getMinScale();
    if (minScale < 0) minScale = qos.getThroughput() > 0 ? 1:0;
    float requestedCpu = parseCpu(kn.getRequestsCpu().isEmpty() ? defaultRequestCpu:kn.getRequestsCpu());
    long requestsMemory = parseMem(kn.getRequestsMemory().isEmpty() ? defaultRequestMem:kn.getRequestsMemory());
    return new CrInstanceSpec(
      minScale,
      kn.getMaxScale(),
      kn.getScaleDownDelay(),
      kn.getTargetConcurrency(),
      requestedCpu,
      requestsMemory,
      parseCpu(kn.getLimitsCpu()),
      parseMem(kn.getLimitsMemory())
    );
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

  private AdjustComponent computeFunc(CrController controller,
                                      ProtoOFunction fn,
                                      SvcPerformanceMetrics metrics) {
    CrDeploymentPlan currentPlan = controller.currentPlan();
    ProtoQosRequirement qos = fn.getQos();
    CrInstanceSpec instanceSpec = currentPlan.fnInstances().get(fn.getKey());
    int targetRps = qos.getThroughput();
    var meanRps = harmonicMean(metrics.rps());
    var meanCpu = mean(metrics.cpu());
    var cpuPerRps = meanRps < 1 ? 0:meanCpu / meanRps; // < 1 is too little. Preventing result explode
    double expectedCpu = targetRps <= 0 ? 0:cpuPerRps * targetRps;
    int expectedInstance = (int) Math.ceil(expectedCpu / instanceSpec.requestsCpu()); // or limit?
    var adjust = instanceSpec.toBuilder().minInstance(expectedInstance).build();
    logger.debug("compute adjust on {} : {} : meanRps {}, meanCpu {}, cpuPerRps {}, expectedInstance {}",
      controller.getId(), fn.getKey(), meanRps, meanCpu, cpuPerRps, expectedInstance);
    var changed = !instanceSpec.equals(adjust);
    logger.debug("compute adjust on {} : {} : ({}) {}",
      controller.getId(), fn.getKey(), changed, adjust);
    return new AdjustComponent(
      changed,
      adjust
    );
  }

  private Map<OprcComponent, CrInstanceSpec> computeCls(CrController controller,
                                                        CrPerformanceMetrics metrics) {
    Map<OprcComponent, CrInstanceSpec> adjustPlanMap = Maps.mutable.empty();
    var cls = controller.getAttachedCls().values().iterator().next();
    var adjust = computeInvoker(controller, cls, metrics.coreMetrics().get(OprcComponent.INVOKER));
    if (adjust.change)
      adjustPlanMap.put(OprcComponent.INVOKER, adjust.spec);
    return adjustPlanMap;
  }

  private AdjustComponent computeInvoker(CrController controller,
                                         ProtoOClass cls,
                                         SvcPerformanceMetrics metrics) {
    CrDeploymentPlan currentPlan = controller.currentPlan();
    ProtoQosRequirement qos = cls.getQos();
    CrInstanceSpec instanceSpec = currentPlan.coreInstances().get(OprcComponent.INVOKER);
    int targetRps = qos.getThroughput();
    var meanRps = harmonicMean(metrics.rps());
    var meanCpu = mean(metrics.cpu());
    var cpuPerRps = meanRps < 1 ? 0:meanCpu / meanRps; // < 1 is too little. Preventing result explode
    double expectedCpu = targetRps <= 0 ? 1:cpuPerRps * targetRps;
    int expectedInstance = (int) Math.ceil(expectedCpu / instanceSpec.requestsCpu()); // or limit?
    if (expectedInstance <= 0) expectedInstance = 1;
    var adjust = instanceSpec.toBuilder().minInstance(expectedInstance).build();
    logger.debug("compute adjust on {} : INVOKER : meanRps {}, meanCpu {}, cpuPerRps {}, expectedInstance {}",
      controller.getId(), meanRps, meanCpu, cpuPerRps, expectedInstance);
    var changed = !instanceSpec.equals(adjust);
    logger.debug("compute adjust on {} : INVOKER : ({}) {}",
      controller.getId(), changed, adjust);
    return new AdjustComponent(
      changed,
      adjust
    );
  }

  public record AdjustComponent(boolean change, CrInstanceSpec spec) {
  }
}
