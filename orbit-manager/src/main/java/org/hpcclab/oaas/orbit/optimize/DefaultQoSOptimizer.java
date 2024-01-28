package org.hpcclab.oaas.orbit.optimize;

import org.hpcclab.oaas.orbit.OprcComponent;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.proto.DeploymentUnit;

import java.util.Map;
import java.util.stream.Collectors;

public class DefaultQoSOptimizer implements QosOptimizer {
  @Override
  public OrbitDeploymentPlan resolve(DeploymentUnit unit) {
    var instances = Map.of(
      OprcComponent.LOADBALANCER, 0,
      OprcComponent.INVOKER, 1,
      OprcComponent.STORAGE_ADAPTER, 1
    );
    var fnInstances = unit.getFnListList()
      .stream()
      .map(f -> Map.entry(f.getKey(), 1))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new OrbitDeploymentPlan(
      instances, fnInstances
    );
  }
}
