package org.hpcclab.oaas.orbit.optimize;

import jakarta.inject.Singleton;
import org.hpcclab.oaas.orbit.controller.OrbitController;
import org.hpcclab.oaas.orbit.controller.OrbitOperation;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultFeasibilityChecker implements FeasibilityChecker {
  private static final Logger logger = LoggerFactory.getLogger( DefaultFeasibilityChecker.class );
  @Override
  public boolean deploymentCheck(OprcEnvironment env, OrbitController orbit, OrbitOperation operation) {
    var estimate = operation.estimate();
    boolean feasible = env.usable().hasMore(estimate);
    logger.info("orbit[{}] require {}, with feasibility [{}], with usable {}",
      orbit.getId(), estimate, feasible, env.usable());
    return feasible;
  }
}
