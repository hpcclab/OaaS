package org.hpcclab.oaas.crm.optimize;

import jakarta.inject.Singleton;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.controller.OrbitOperation;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultFeasibilityChecker implements FeasibilityChecker {
  private static final Logger logger = LoggerFactory.getLogger( DefaultFeasibilityChecker.class );
  @Override
  public boolean deploymentCheck(OprcEnvironment env, CrController orbit, OrbitOperation operation) {
    var estimate = operation.estimate();
    boolean feasible = env.usable().hasMore(estimate);
    logger.info("orbit[{}] require {}, with feasibility [{}], with usable {}",
      orbit.getId(), estimate, feasible, env.usable());
    return feasible;
  }
}
