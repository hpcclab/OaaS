package org.hpcclab.oaas.crm.optimize;

import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.SvcPerformanceMetrics;
import org.hpcclab.oaas.proto.*;

import java.util.Map;
import java.util.stream.Collectors;

public class DefaultQoSOptimizer implements QosOptimizer {
  @Override
  public CrDeploymentPlan resolve(DeploymentUnit unit) {
    var instances = Map.of(
      OprcComponent.LOADBALANCER, makeCore(),
      OprcComponent.INVOKER, makeCore(),
      OprcComponent.STORAGE_ADAPTER, makeCore()
    );
    var fnInstances = unit.getFnListList()
      .stream()
      .map(f -> Map.entry(f.getKey(), convert(f.getProvision())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new CrDeploymentPlan(
      instances, fnInstances
    );
  }

  CrInstanceSpec makeCore() {
    return new CrInstanceSpec(
      1,-1,
      null,
      -1
    );
  }
  CrInstanceSpec convert(ProvisionConfig provision) {
    var kn = provision.getKnative();
    return new CrInstanceSpec(
      kn.getMinScale(),
      kn.getMaxScale(),
      kn.getScaleDownDelay(),
      kn.getTargetConcurrency()
    );
  }

  @Override
  public CrAdjustmentPlan adjust(CrController controller, CrPerformanceMetrics metrics) {
    Map<OprcComponent, CrInstanceSpec> coreInstance = Map.of();
    Map<String, CrInstanceSpec> fnInstance = Maps.mutable.empty();
    for (var entry : controller.getAttachedFn().entrySet()) {
      var fnKey = entry.getKey();
      var fn = entry.getValue();
      var fnMetrics = metrics.fnMetrics().get(fnKey);
      if (fnMetrics == null) continue;
      var adj = computeFunc(controller, fn, fnMetrics);
      if (adj.change()) fnInstance.put(fnKey, adj.spec());
    }
    for (var entry : controller.getAttachedCls().entrySet()) {
      var cls = entry.getValue();
      coreInstance = computeCls(controller, cls, metrics);
    }
    var currentPlan = controller.currentPlan();
    if (currentPlan == null)
      return new CrAdjustmentPlan(Map.of(), Map.of(), false);
    return new CrAdjustmentPlan(
      coreInstance,
      fnInstance,
      false);
  }

  AdjustComponent computeFunc(CrController controller,
                              ProtoOFunction fn,
                              SvcPerformanceMetrics metrics) {
    return new AdjustComponent(false, null);
  }
  Map<OprcComponent, CrInstanceSpec> computeCls(CrController controller,
                                         ProtoOClass cls,
                                         CrPerformanceMetrics metrics) {
    return Map.of();
  }
  record AdjustComponent(boolean change, CrInstanceSpec spec){}
}
