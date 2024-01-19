package org.hpcclab.oaas.orbit.rpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.orbit.OrbitTemplateManager;
import org.hpcclab.oaas.proto.*;

@GrpcService
public class OrbitManagerImpl implements OrbitManager {
  OrbitTemplateManager templateManager;
  @GrpcClient("class-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub stateService;

  public OrbitManagerImpl(OrbitTemplateManager templateManager) {
    this.templateManager = templateManager;
  }

  @Override
  @RunOnVirtualThread
  public Uni<ProtoOrbit> deploy(DeploymentUnit deploymentUnit) {
    long orbitId = deploymentUnit.getCls()
      .getStatus().getOrbitId();
    if (orbitId >= 0) {
      var orbit = stateService.get(SingleKeyQuery.newBuilder().setKey(String.valueOf(orbitId)).build());
      var orbitStructure = templateManager.load(deploymentUnit, orbit);
      orbitStructure.update(deploymentUnit);
      return Uni
        .createFrom().item(orbitStructure.dump());
    } else {
      var template = templateManager.selectTemplate(deploymentUnit);
      var orbitStructure = template.create(deploymentUnit);
      try {
        orbitStructure.deployAll();
      } catch (Throwable e) {
        return Uni.createFrom().failure(e);
      }
      return Uni
        .createFrom().item(orbitStructure.dump());
    }
  }

  @Override
  public Uni<OprcResponse> destroy(ProtoOrbit request) {
    // TODO
    return null;
  }
}
