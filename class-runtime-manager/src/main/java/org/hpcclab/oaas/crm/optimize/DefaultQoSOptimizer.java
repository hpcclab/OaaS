package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.proto.DeploymentUnit;

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
  public CrAdjustmentPlan adjust(CrController controller) {
    return null;
  }
}
