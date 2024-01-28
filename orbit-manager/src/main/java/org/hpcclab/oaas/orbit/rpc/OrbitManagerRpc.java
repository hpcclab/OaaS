package org.hpcclab.oaas.orbit.rpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.bouncycastle.cert.ocsp.Req;
import org.hpcclab.oaas.orbit.OrbitTemplateManager;
import org.hpcclab.oaas.orbit.controller.OrbitController;
import org.hpcclab.oaas.orbit.controller.OrbitOperation;
import org.hpcclab.oaas.orbit.env.EnvironmentManager;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.exception.OrbitDeployException;
import org.hpcclab.oaas.orbit.optimize.FeasibilityChecker;
import org.hpcclab.oaas.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class OrbitManagerRpc implements OrbitManager {
  private static final Logger logger = LoggerFactory.getLogger(OrbitManagerRpc.class);
  OrbitTemplateManager templateManager;
  EnvironmentManager environmentManager;
  FeasibilityChecker feasibilityChecker;
  @GrpcClient("class-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub stateService;

  @Inject
  public OrbitManagerRpc(OrbitTemplateManager templateManager,
                         EnvironmentManager environmentManager,
                         FeasibilityChecker feasibilityChecker) {
    this.templateManager = templateManager;
    this.environmentManager = environmentManager;
    this.feasibilityChecker = feasibilityChecker;
  }

  @Override
  @RunOnVirtualThread
  public Uni<ProtoOrbit> deploy(DeploymentUnit deploymentUnit) {
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
  public Uni<ProtoOrbit> update(OrbitUpdateRequest request) {
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

  private Uni<ProtoOrbit> deployOrNothing(OrbitController orbitController,
                                          OrbitOperation operation,
                                          OprcEnvironment env) {
    var feasible = feasibilityChecker.deploymentCheck(env, orbitController, operation);
    if (!feasible)
      return Uni.createFrom().failure(new OrbitDeployException("Not feasible"));
    try {
      operation.apply();
      return Uni
        .createFrom().item(orbitController.dump());
    } catch (Throwable e) {
      logger.error("orbit deploying error and attempt to clean up", e);
      operation.rollback();
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<OprcResponse> destroy(ProtoOrbit orbit) {
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
  public Uni<ProtoOrbit> detach(DetachOrbitRequest request) {
    var env = environmentManager.getEnvironment();
    var orbitStructure = templateManager.load(env, request.getOrbit());
    var operation = orbitStructure.createDetachOperation(request.getCls());
    try {
      operation.apply();
      return Uni.createFrom().item(orbitStructure.dump());
    } catch (Throwable e) {
      return Uni.createFrom().failure(e);
    }
  }
}
