package org.hpcclab.oaas.crm.optimize;

import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.SvcPerformanceMetrics;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.OprcFunction;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.Map;
import java.util.stream.Collectors;

public class DefaultQoSOptimizer implements QosOptimizer {
  @Override
  public CrDeploymentPlan resolve(DeploymentUnit unit) {
    var instances = Map.of(
      OprcComponent.LOADBALANCER, 0,
      OprcComponent.INVOKER, 1,
      OprcComponent.STORAGE_ADAPTER, 1
    );
    var fnInstances = unit.getFnListList()
      .stream()
      .map(f -> Map.entry(f.getKey(), f.getProvision().getKnative().getImage().isEmpty()? 1: 0))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new CrDeploymentPlan(
      instances, fnInstances
    );
  }

  @Override
  public CrAdjustmentPlan adjust(CrController controller, CrPerformanceMetrics metrics) {
    Map<OprcComponent, Integer> coreInstance = Map.of();
    Map<String, Integer> fnInstance = Maps.mutable.empty();
    for (var entry : controller.getAttachedFn().entrySet()) {
      var fnKey = entry.getKey();
      var fn = entry.getValue();
      var fnMetrics = metrics.fnMetrics().get(fnKey);
      if (fnMetrics == null) continue;
      var adj = computeFunc(controller, fn, fnMetrics);
      if (adj.change()) fnInstance.put(fnKey, adj.instance());
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
    return new AdjustComponent(false, -1 );
  }
  Map<OprcComponent, Integer> computeCls(CrController controller,
                                         ProtoOClass cls,
                                         CrPerformanceMetrics metrics) {
    return Map.of();
  }
  record AdjustComponent(boolean change, int instance){}
}
