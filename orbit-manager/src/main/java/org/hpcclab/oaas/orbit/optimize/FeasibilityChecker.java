package org.hpcclab.oaas.orbit.optimize;

import org.hpcclab.oaas.orbit.controller.OrbitController;
import org.hpcclab.oaas.orbit.controller.OrbitOperation;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;

public interface FeasibilityChecker {
  boolean deploymentCheck(OprcEnvironment env,
                          OrbitController orbit,
                          OrbitOperation operation);
}
