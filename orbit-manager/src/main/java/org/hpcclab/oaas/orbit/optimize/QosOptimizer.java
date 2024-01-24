package org.hpcclab.oaas.orbit.optimize;

import org.hpcclab.oaas.proto.DeploymentUnit;

public interface QosOptimizer {
  OrbitDeploymentPlan resolve(DeploymentUnit unit);
}
