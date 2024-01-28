package org.hpcclab.oaas.orbit.optimize;

import org.hpcclab.oaas.orbit.OprcComponent;

import java.util.Map;

public record OrbitDeploymentPlan(
  Map<OprcComponent, Integer> coreInstances,
  Map<String, Integer> fnInstances
) {
}
