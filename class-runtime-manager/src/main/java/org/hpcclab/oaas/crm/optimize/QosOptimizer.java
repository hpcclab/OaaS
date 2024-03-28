package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics;
import org.hpcclab.oaas.proto.DeploymentUnit;

public interface QosOptimizer {
  CrDeploymentPlan resolve(DeploymentUnit unit, OprcEnvironment environment);
  CrAdjustmentPlan adjust(CrController controller, CrPerformanceMetrics metrics);
}
