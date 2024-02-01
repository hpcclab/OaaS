package org.hpcclab.oaas.controller.service;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.arango.RepoFactory;
import org.hpcclab.oaas.arango.repo.GenericArgRepository;
import org.hpcclab.oaas.controller.model.OprcCr;
import org.hpcclab.oaas.controller.model.CrMapperImpl;
import org.hpcclab.oaas.controller.model.CrMapper;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.proto.*;

import static org.hpcclab.oaas.arango.AutoRepoBuilder.confRegistry;

@ApplicationScoped
public class OrbitStateManager {
  @Inject
  CrMapper crMapper;
  @Inject
  ProtoMapper protoMapper;
  GenericArgRepository<OprcCr> repo;
  @GrpcClient("orbit-manager")
  CrManagerGrpc.CrManagerBlockingStub orbitManager;

  @Inject
  public OrbitStateManager() {
    var fac = new RepoFactory(confRegistry.getConfMap().get("PKG"));
    repo = fac.createGenericRepo(OprcCr.class, OprcCr::getKey, "orbit");
    repo.createIfNotExist();
    crMapper = new CrMapperImpl();
  }

  public GenericArgRepository<OprcCr> getRepo() {
    return repo;
  }

  public Uni<OprcResponse> updateOrbit(ProtoCr protoOrbit) {
    var orbit = crMapper.map(protoOrbit);
    return repo.persistAsync(orbit)
      .map(entity -> OprcResponse.newBuilder()
        .setSuccess(true)
        .build());
  }

  public Uni<ProtoCr> get(String id) {
    return repo.getAsync(id)
      .map(doc -> crMapper.map(doc));
  }

  public Multi<ProtoCr> listOrbit(PaginateQuery request) {
    return repo.getQueryService()
      .paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(p -> Multi.createFrom().iterable(p.getItems()))
      .map(doc -> crMapper.map(doc));
  }

  public void detach(OClass cls) {
    var orbit = getRepo().get(OprcCr.toKey(cls.getStatus().getOrbitId()));
    var newOrbit = orbitManager.detach(DetachCrRequest.newBuilder()
      .setOrbit(crMapper.map(orbit)).setCls(
        protoMapper.toProto(cls)
      ).build());
    if (newOrbit.getAttachedClsList().isEmpty()) {
      repo.remove(OprcCr.toKey(newOrbit.getId()));
    } else {
      repo.persistAsync(crMapper.map(newOrbit));
    }
    cls.getStatus().setOrbitId(0);
  }
}
