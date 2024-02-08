package org.hpcclab.oaas.crm.optimize;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.controller.CrOperation;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.proto.CrOperationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OperationExecutor {
  private static final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

  FeasibilityChecker feasibilityChecker;

  @Inject
  public OperationExecutor(FeasibilityChecker feasibilityChecker) {
    this.feasibilityChecker = feasibilityChecker;
  }

  public Uni<CrOperationResponse> applyOrRollback(CrController crController,
                                                  CrOperation operation,
                                                  OprcEnvironment env) {
    var feasible = feasibilityChecker.deploymentCheck(env, crController, operation);
    if (!feasible)
      return Uni.createFrom().failure(new CrDeployException("Not feasible"));
    try {
      operation.apply();
      var cr = crController.dump();
      CrOperationResponse response = CrOperationResponse.newBuilder()
        .setCr(cr)
        .addAllClsUpdates(operation.stateUpdates().clsUpdates())
        .addAllFnUpdates(operation.stateUpdates().fnUpdates())
        .build();
      return Uni
        .createFrom().item(response);
    } catch (Throwable e) {
      logger.error("CR operation execution error and attempt to clean up", e);
      operation.rollback();
      return Uni.createFrom().failure(e);
    }
  }

  public void applyOrThrow(CrController crController,
                           CrOperation operation,
                           OprcEnvironment env) {
    var feasible = feasibilityChecker.deploymentCheck(env, crController, operation);
    if (!feasible)
      throw new CrDeployException("Not feasible");
    try {
      operation.apply();
    } catch (Throwable e) {
      logger.error("CR operation execution and attempt to clean up", e);
      operation.rollback();
      throw e;
    }
  }


}
