package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.controller.CrOperation;
import org.hpcclab.oaas.crm.env.OprcEnvironment;

public interface FeasibilityChecker {
  boolean deploymentCheck(OprcEnvironment env,
                          CrController orbit,
                          CrOperation operation);
}
