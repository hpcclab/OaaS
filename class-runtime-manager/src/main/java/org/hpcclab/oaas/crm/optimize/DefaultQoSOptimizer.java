package org.hpcclab.oaas.crm.optimize;

import io.fabric8.kubernetes.api.model.Quantity;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.SvcPerformanceMetrics;
import org.hpcclab.oaas.proto.*;
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
      .map(f -> Map.entry(f.getKey(), convert(f.getProvision())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new CrDeploymentPlan(
      instances, fnInstances
    );
  }


  CrInstanceSpec convert(ProtoProvisionConfig provision) {
    var kn = provision.getKnative();
    float requestedCpu = parseCpu(kn.getRequestsCpu().isEmpty() ? defaultRequestCpu:kn.getRequestsCpu());
    long requestsMemory = parseMem(kn.getRequestsMemory().isEmpty() ? defaultRequestMem:kn.getRequestsMemory());
    return new CrInstanceSpec(
      kn.getMinScale(),
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

  @Override
  public CrAdjustmentPlan adjust(CrController controller, CrPerformanceMetrics metrics) {
    Map<OprcComponent, CrInstanceSpec> coreInstance = Map.of();
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
    for (var entry : controller.getAttachedCls().entrySet()) {
      var cls = entry.getValue();
      coreInstance = computeCls(controller, cls, metrics);
    }
    if (currentPlan==null)
      return new CrAdjustmentPlan(Map.of(), Map.of(), false);
    return new CrAdjustmentPlan(
      coreInstance,
      fnInstance,
      false);
  }

  AdjustComponent computeFunc(CrController controller,
                              ProtoOFunction fn,
                              SvcPerformanceMetrics metrics) {
    CrDeploymentPlan currentPlan = controller.currentPlan();
    ProtoQosRequirement qos = fn.getQos();
    CrInstanceSpec instanceSpec = currentPlan.fnInstances().get(fn.getKey());
    int throughput = qos.getThroughput();
    var meanRps = harmonicMean(metrics.rps());
    var meanCpu = mean(metrics.cpu());
    var cpuPerRps = meanRps==0 ? 0:meanCpu / meanRps;
    double expectedCpu = throughput <= 0 ? 0: cpuPerRps / throughput;
    int expectedInstance = (int) Math.ceil(expectedCpu / instanceSpec.requestsCpu());
    var adjust = instanceSpec.toBuilder().minInstance(expectedInstance).build();
    var changed = expectedCpu==instanceSpec.minInstance();
    logger.debug("compute adjust on {} : {} : meanRps {}, meanCpu {}, cpuPerRps {}",
      controller.getId(), fn.getKey(), meanRps, meanCpu, cpuPerRps);
    logger.debug("compute adjust on {} : {} : ({}) {}",
      controller.getId(), fn.getKey(), changed, adjust);
    return new AdjustComponent(
      changed,
      adjust
    );
  }

  Map<OprcComponent, CrInstanceSpec> computeCls(CrController controller,
                                                ProtoOClass cls,
                                                CrPerformanceMetrics metrics) {
    return Map.of();
  }


  record AdjustComponent(boolean change, CrInstanceSpec spec) {
  }
}
