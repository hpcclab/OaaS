package org.hpcclab.oaas.crm.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.optimize.OperationExecutor;
import org.hpcclab.oaas.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class CrManagerRpc implements CrManager {
  private static final Logger logger = LoggerFactory.getLogger(CrManagerRpc.class);
  EnvironmentManager environmentManager;
  OperationExecutor operationExecutor;
  CrControllerManager controllerManager;

  @Inject
  public CrManagerRpc(EnvironmentManager environmentManager,
                      OperationExecutor operationExecutor,
                      CrControllerManager controllerManager) {
    this.environmentManager = environmentManager;
    this.operationExecutor = operationExecutor;
    this.controllerManager = controllerManager;
  }

  @Override
  @RunOnVirtualThread
  public Uni<CrOperationResponse> deploy(DeploymentUnit deploymentUnit) {
    try {
      long crId = deploymentUnit.getCls()
        .getStatus().getCrId();
      var env = environmentManager.getEnvironment();
      if (crId > 0) {
        var controller = controllerManager.getOrLoad(crId, env);
        var plan = controller.getTemplate().getQosOptimizer()
          .resolve(deploymentUnit, env);
        var operation = controller.createUpdateOperation(plan, deploymentUnit);
        return operationExecutor.applyOrRollback(controller, operation, env);
      } else {
        var controller = controllerManager.create(env, deploymentUnit);
        var plan = controller.getTemplate().getQosOptimizer()
          .resolve(deploymentUnit, env);
        var operation = controller.createDeployOperation(plan, deploymentUnit);
        return operationExecutor.applyOrRollback(controller, operation, env);
      }
    } catch (Exception e) {
      logger.error("orbit deploying error", e);
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<CrOperationResponse> update(CrUpdateRequest request) {
    try {
      var env = environmentManager.getEnvironment();
      var orbitStructure = controllerManager.getOrLoad(request.getOrbit(), env);
      var plan = orbitStructure.getTemplate().getQosOptimizer()
        .resolve(request.getUnit(), env);
      var operation = orbitStructure.createUpdateOperation(plan, request.getUnit());
      return operationExecutor.applyOrRollback(orbitStructure, operation, env);
    } catch (Exception e) {
      logger.error("orbit deploying error", e);
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<OprcResponse> destroy(ProtoCr orbit) {
    try {
      var env = environmentManager.getEnvironment();
      var controller = controllerManager.getOrLoad(orbit, env);
      var operation = controller.createDestroyOperation();
      operation.apply();
      controllerManager.deleteFromLocal(controller);
      return Uni.createFrom().item(OprcResponse.newBuilder().setSuccess(true).build());
    } catch (Exception e) {
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<CrOperationResponse> detach(DetachCrRequest request) {
    var env = environmentManager.getEnvironment();
    var crController = controllerManager.getOrLoad(request.getOrbit(), env);
    var operation = crController.createDetachOperation(request.getCls());
    try {
      operation.apply();
      var uni = operationExecutor.applyOrRollback(crController, operation, env);
      return uni;
    } catch (Exception e) {
      return Uni.createFrom().failure(e);
    }
  }
}
