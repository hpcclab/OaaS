package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.controller.OrbitOperation;
import org.hpcclab.oaas.crm.env.OprcEnvironment;

public interface FeasibilityChecker {
  boolean deploymentCheck(OprcEnvironment env,
                          CrController orbit,
                          OrbitOperation operation);
}
