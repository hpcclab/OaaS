package org.hpcclab.oaas.controller.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.arango.RepoFactory;
import org.hpcclab.oaas.arango.repo.GenericArgRepository;
import org.hpcclab.oaas.controller.model.Orbit;
import org.hpcclab.oaas.controller.model.OrbitMapper;
import org.hpcclab.oaas.controller.model.OrbitMapperImpl;
import org.hpcclab.oaas.controller.service.OrbitStateManager;
import org.hpcclab.oaas.proto.*;

import static org.hpcclab.oaas.arango.AutoRepoBuilder.confRegistry;

@GrpcService
public class OrbitStateServiceImpl implements OrbitStateUpdater, OrbitStateService {
  OrbitStateManager stateManager;

  @Inject
  public OrbitStateServiceImpl(OrbitStateManager stateManager) {
    this.stateManager = stateManager;
  }

  @Override
  public Uni<OprcResponse> updateOrbit(ProtoOrbit protoOrbit) {
    return stateManager.updateOrbit(protoOrbit);
  }

  @Override
  public Uni<ProtoOrbit> get(SingleKeyQuery request) {
    return stateManager.get(request.getKey());
  }

  @Override
  public Multi<ProtoOrbit> listOrbit(PaginateQuery request) {
    return stateManager.listOrbit(request);
  }
}
