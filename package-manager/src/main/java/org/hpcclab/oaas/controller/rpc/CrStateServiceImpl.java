package org.hpcclab.oaas.controller.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.controller.service.CrStateManager;
import org.hpcclab.oaas.proto.*;

@GrpcService
public class CrStateServiceImpl implements InternalCrStateService, CrStateService {
  CrStateManager stateManager;
  @Inject
  public CrStateServiceImpl(CrStateManager stateManager) {
    this.stateManager = stateManager;
  }

  @Override
  @RunOnVirtualThread
  public Uni<ProtoCr> get(SingleKeyQuery request) {
    return stateManager.get(request.getKey());
  }

  @Override
  public Multi<ProtoCr> list(PaginateQuery request) {
    return stateManager.listCr(request);
  }

  @Override
  public Uni<OprcResponse> updateCr(ProtoCr request) {
    return stateManager.updateCr(request);
  }

  @Override
  public Uni<ProtoCrHash> updateHash(ProtoCrHash request) {
    return stateManager.updateCrHash(request);
  }

  @Override
  public Uni<ProtoCrHash> getHash(SingleKeyQuery request) {
    return stateManager.getHash(request.getKey());
  }

  @Override
  public Multi<ProtoCrHash> listHash(PaginateQuery request) {
    return stateManager.listHash(request);
  }
}
