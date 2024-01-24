package org.hpcclab.oaas.orbit.optimize;

import org.hpcclab.oaas.orbit.OprcComponent;
import org.hpcclab.oaas.orbit.env.EnvResource;

import java.util.Map;

public record OrbitAdjustmentPlan(
  OrbitDeploymentPlan current,
  Map<OprcComponent, Integer> instances,
  EnvResource requirement
) {
}
