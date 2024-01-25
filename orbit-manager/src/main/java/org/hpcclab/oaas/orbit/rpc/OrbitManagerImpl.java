package org.hpcclab.oaas.orbit.rpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.orbit.OrbitStructure;
import org.hpcclab.oaas.orbit.OrbitTemplateManager;
import org.hpcclab.oaas.orbit.env.EnvironmentManager;
import org.hpcclab.oaas.orbit.optimize.OrbitDeploymentPlan;
import org.hpcclab.oaas.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class OrbitManagerImpl implements OrbitManager {
  private static final Logger logger = LoggerFactory.getLogger(OrbitManagerImpl.class);
  OrbitTemplateManager templateManager;
  EnvironmentManager environmentManager;
  @GrpcClient("class-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub stateService;

  @Inject
  public OrbitManagerImpl(OrbitTemplateManager templateManager,
                          EnvironmentManager environmentManager) {
    this.templateManager = templateManager;
    this.environmentManager = environmentManager;
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
        orbitStructure.update(deploymentUnit);
        return Uni
          .createFrom().item(orbitStructure.dump());
      } else {
        var template = templateManager.selectTemplate(env, deploymentUnit);
        var orbitStructure = template.create(env, deploymentUnit);
        var plan = orbitStructure.createPlan(deploymentUnit);
        return deployOrNothing(orbitStructure, plan, deploymentUnit);
      }
    } catch (Throwable e) {
      logger.error("orbit deploying error", e);
      return Uni.createFrom().failure(e);
    }
  }

  private Uni<ProtoOrbit> deployOrNothing(OrbitStructure orbitStructure,
                               OrbitDeploymentPlan plan,
                               DeploymentUnit unit) throws Throwable {
    try {
      orbitStructure.deployAll(plan, unit);
      return Uni
        .createFrom().item(orbitStructure.dump());
    } catch (Throwable e) {
      logger.error("orbit deploying error and attempt to clean up", e);
      orbitStructure.destroy();
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<OprcResponse> destroy(ProtoOrbit orbit) {
    try {
      var env = environmentManager.getEnvironment();
      if (orbit.getId() > 0) {
        var orbitStructure = templateManager.load(env, orbit);
        orbitStructure.destroy();
      }
      return Uni.createFrom().item(OprcResponse.newBuilder().setSuccess(true).build());
    } catch (Throwable e) {
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<ProtoOrbit> detach(DetachOrbitRequest request) {
    try {
      var env = environmentManager.getEnvironment();
      var orbitStructure = templateManager.load(env, request.getOrbit());
      orbitStructure.detach(request.getCls());
      return Uni.createFrom().item(orbitStructure.dump());
    } catch (Throwable e) {
      return Uni.createFrom().failure(e);
    }
  }
}
