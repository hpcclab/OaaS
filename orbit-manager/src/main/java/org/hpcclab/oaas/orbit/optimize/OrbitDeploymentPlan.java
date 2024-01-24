package org.hpcclab.oaas.orbit.optimize;

import org.hpcclab.oaas.orbit.OprcComponent;
import org.hpcclab.oaas.orbit.env.EnvResource;

import java.util.Map;

public record OrbitDeploymentPlan(
  Map<OprcComponent, Integer> coreInstances,
  Map<String, Integer> fnInstances,
  EnvResource requirement
) {
}
