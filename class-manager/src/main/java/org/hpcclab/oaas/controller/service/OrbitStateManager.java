package org.hpcclab.oaas.controller.service;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.arango.RepoFactory;
import org.hpcclab.oaas.arango.repo.GenericArgRepository;
import org.hpcclab.oaas.controller.model.Orbit;
import org.hpcclab.oaas.controller.model.OrbitMapper;
import org.hpcclab.oaas.controller.model.OrbitMapperImpl;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.proto.*;

import static org.hpcclab.oaas.arango.AutoRepoBuilder.confRegistry;

@ApplicationScoped
public class OrbitStateManager {
  @Inject
  OrbitMapper orbitMapper;
  @Inject
  ProtoMapper protoMapper;
  GenericArgRepository<Orbit> repo;
  @GrpcClient("orbit-manager")
  OrbitManagerGrpc.OrbitManagerBlockingStub orbitManager;

  @Inject
  public OrbitStateManager() {
    var fac = new RepoFactory(confRegistry.getConfMap().get("PKG"));
    repo = fac.createGenericRepo(Orbit.class, Orbit::getKey, "orbit");
    repo.createIfNotExist();
    orbitMapper = new OrbitMapperImpl();
  }

  public GenericArgRepository<Orbit> getRepo() {
    return repo;
  }

  public Uni<OprcResponse> updateOrbit(ProtoOrbit protoOrbit) {
    var orbit = orbitMapper.map(protoOrbit);
    return repo.persistAsync(orbit)
      .map(entity -> OprcResponse.newBuilder()
        .setSuccess(true)
        .build());
  }

  public Uni<ProtoOrbit> get(String id) {
    return repo.getAsync(id)
      .map(doc -> orbitMapper.map(doc));
  }

  public Multi<ProtoOrbit> listOrbit(PaginateQuery request) {
    return repo.getQueryService()
      .paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(p -> Multi.createFrom().iterable(p.getItems()))
      .map(doc -> orbitMapper.map(doc));
  }

  public void detach(OClass cls) {
    var orbit = getRepo().get(String.valueOf(cls.getStatus().getOrbitId()));
    var newOrbit = orbitManager.detach(DetachOrbitRequest.newBuilder()
      .setOrbit(orbitMapper.map(orbit)).setCls(
        protoMapper.toProto(cls)
      ).build());
    if (newOrbit.getAttachedClsList().isEmpty()) {
      repo.remove(String.valueOf(newOrbit.getId()));
    } else {
      repo.persistAsync(orbitMapper.map(newOrbit));
    }
    cls.getStatus().setOrbitId(0);
  }
}
