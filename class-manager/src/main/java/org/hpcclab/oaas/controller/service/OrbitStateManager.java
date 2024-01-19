package org.hpcclab.oaas.controller.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.arango.RepoFactory;
import org.hpcclab.oaas.arango.repo.GenericArgRepository;
import org.hpcclab.oaas.controller.model.Orbit;
import org.hpcclab.oaas.controller.model.OrbitMapper;
import org.hpcclab.oaas.controller.model.OrbitMapperImpl;
import org.hpcclab.oaas.proto.OprcResponse;
import org.hpcclab.oaas.proto.PaginateQuery;
import org.hpcclab.oaas.proto.ProtoOrbit;

import static org.hpcclab.oaas.arango.AutoRepoBuilder.confRegistry;

@ApplicationScoped
public class OrbitStateManager {
  OrbitMapper mapper;
  GenericArgRepository<Orbit> repo;

  @Inject
  public OrbitStateManager() {
    var fac = new RepoFactory(confRegistry.getConfMap().get("PKG"));
    repo = fac.createGenericRepo(Orbit.class, Orbit::getKey, "orbit");
    repo.createIfNotExist();
    mapper = new OrbitMapperImpl();
  }


  public Uni<OprcResponse> updateOrbit(ProtoOrbit protoOrbit) {
    var orbit = mapper.map(protoOrbit);
    return repo.persistAsync(orbit)
      .map(entity -> OprcResponse.newBuilder()
        .setSuccess(true)
        .build());
  }

  public Uni<ProtoOrbit> get(String id) {
    return repo.getAsync(id)
      .map(doc -> mapper.map(doc));
  }

  public Multi<ProtoOrbit> listOrbit(PaginateQuery request) {
    return repo.getQueryService()
      .paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(p -> Multi.createFrom().iterable(p.getItems()))
      .map(doc -> mapper.map(doc));
  }
}
