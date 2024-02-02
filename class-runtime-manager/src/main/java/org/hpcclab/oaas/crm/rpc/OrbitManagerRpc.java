package org.hpcclab.oaas.crm.rpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.crm.template.CrTemplateManager;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.controller.CrOperation;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.optimize.FeasibilityChecker;
import org.hpcclab.oaas.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@GrpcService
public class OrbitManagerRpc implements CrManager {
  private static final Logger logger = LoggerFactory.getLogger(OrbitManagerRpc.class);
  CrTemplateManager templateManager;
  EnvironmentManager environmentManager;
  FeasibilityChecker feasibilityChecker;
  @GrpcClient("package-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub stateService;

  @Inject
  public OrbitManagerRpc(CrTemplateManager templateManager,
                         EnvironmentManager environmentManager,
                         FeasibilityChecker feasibilityChecker) {
    this.templateManager = templateManager;
    this.environmentManager = environmentManager;
    this.feasibilityChecker = feasibilityChecker;
  }

  @Override
  @RunOnVirtualThread
  public Uni<CrOperationResponse> deploy(DeploymentUnit deploymentUnit) {
    try {
      long orbitId = deploymentUnit.getCls()
        .getStatus().getOrbitId();
      var env = environmentManager.getEnvironment();
      if (orbitId > 0) {
        var orbit = stateService.get(SingleKeyQuery.newBuilder().setKey(String.valueOf(orbitId)).build());
        var orbitStructure = templateManager.load(env, orbit);
        var plan = orbitStructure.createPlan(deploymentUnit);
        var operation = orbitStructure.createUpdateOperation(plan, deploymentUnit);
        return deployOrNothing(orbitStructure, operation, env);
      } else {
        var template = templateManager.selectTemplate(env, deploymentUnit);
        var orbitStructure = template.create(env, deploymentUnit);
        var plan = orbitStructure.createPlan(deploymentUnit);
        var operation = orbitStructure.createDeployOperation(plan, deploymentUnit);
        return deployOrNothing(orbitStructure, operation, env);
      }
    } catch (Throwable e) {
      logger.error("orbit deploying error", e);
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<CrOperationResponse> update(CrUpdateRequest request) {
    try {
      var env = environmentManager.getEnvironment();
      var orbitStructure = templateManager.load(env, request.getOrbit());
      var plan = orbitStructure.createPlan(request.getUnit());
      var operation = orbitStructure.createUpdateOperation(plan, request.getUnit());
      return deployOrNothing(orbitStructure, operation, env);
    } catch (Throwable e) {
      logger.error("orbit deploying error", e);
      return Uni.createFrom().failure(e);
    }
  }

  private Uni<CrOperationResponse> deployOrNothing(CrController crController,
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
      logger.error("orbit deploying error and attempt to clean up", e);
      operation.rollback();
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<OprcResponse> destroy(ProtoCr orbit) {
    try {
      var env = environmentManager.getEnvironment();
      var orbitStructure = templateManager.load(env, orbit);
      var operation = orbitStructure.createDestroyOperation();
      operation.apply();
      return Uni.createFrom().item(OprcResponse.newBuilder().setSuccess(true).build());
    } catch (Throwable e) {
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<CrOperationResponse> detach(DetachCrRequest request) {
    var env = environmentManager.getEnvironment();
    var crController = templateManager.load(env, request.getOrbit());
    var operation = crController.createDetachOperation(request.getCls());
    try {
      operation.apply();
      return deployOrNothing(crController, operation, env);
    } catch (Throwable e) {
      return Uni.createFrom().failure(e);
    }
  }
}
