package org.hpcclab.oaas.controller.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.controller.service.CrStateManager;
import org.hpcclab.oaas.proto.*;

@GrpcService
public class OrbitStateServiceImpl implements CrStateUpdater, OrbitStateService {
  CrStateManager stateManager;

  @Inject
  public OrbitStateServiceImpl(CrStateManager stateManager) {
    this.stateManager = stateManager;
  }

  @Override
  public Uni<ProtoCr> get(SingleKeyQuery request) {
    return stateManager.get(request.getKey());
  }

  @Override
  public Multi<ProtoCr> listOrbit(PaginateQuery request) {
    return stateManager.listOrbit(request);
  }

  @Override
  public Uni<OprcResponse> updateOrbit(ProtoCr request) {
    return stateManager.updateCr(request);
  }
}
