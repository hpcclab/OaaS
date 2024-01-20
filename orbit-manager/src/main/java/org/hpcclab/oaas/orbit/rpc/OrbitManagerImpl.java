package org.hpcclab.oaas.orbit.rpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.orbit.OrbitTemplateManager;
import org.hpcclab.oaas.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class OrbitManagerImpl implements OrbitManager {
  private static final Logger logger = LoggerFactory.getLogger( OrbitManagerImpl.class );
  OrbitTemplateManager templateManager;
  @GrpcClient("class-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub stateService;

  public OrbitManagerImpl(OrbitTemplateManager templateManager) {
    this.templateManager = templateManager;
  }

  @Override
  @RunOnVirtualThread
  public Uni<ProtoOrbit> deploy(DeploymentUnit deploymentUnit) {
    try {
      long orbitId = deploymentUnit.getCls()
        .getStatus().getOrbitId();
      if (orbitId > 0) {
        var orbit = stateService.get(SingleKeyQuery.newBuilder().setKey(String.valueOf(orbitId)).build());
        var orbitStructure = templateManager.load(orbit);
        orbitStructure.update(deploymentUnit);
        return Uni
          .createFrom().item(orbitStructure.dump());
      } else {
        var template = templateManager.selectTemplate(deploymentUnit);
        var orbitStructure = template.create(deploymentUnit);
        orbitStructure.deployAll();
        return Uni
          .createFrom().item(orbitStructure.dump());
      }
    } catch (Throwable e) {
      logger.error("orbit deploying error", e);
      return Uni.createFrom().failure(e);
    }
  }

  @Override
  @RunOnVirtualThread
  public Uni<OprcResponse> destroy(ProtoOrbit orbit) {
    try {
      if (orbit.getId() > 0) {
        var orbitStructure = templateManager.load(orbit);
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
      var orbitStructure = templateManager.load(request.getOrbit());
      orbitStructure.detach(request.getCls());
      return Uni.createFrom().item(orbitStructure.dump());
    } catch (Throwable e) {
      return Uni.createFrom().failure(e);
    }
  }
}
