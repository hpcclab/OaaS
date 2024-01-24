package org.hpcclab.oaas.orbit.optimize;

import org.hpcclab.oaas.orbit.OrbitStructure;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.optimize.OrbitDeploymentPlan;

public interface FeasibilityChecker {
  boolean deploymentCheck(OrbitDeploymentPlan plan, OprcEnvironment env, OrbitStructure orbit);
}
