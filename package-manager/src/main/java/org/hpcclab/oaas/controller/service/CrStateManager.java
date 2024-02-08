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
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
public class CrStateManager {
  private static final Logger logger = LoggerFactory.getLogger( CrStateManager.class );
  @Inject
  CrMapper crMapper;
  @Inject
  ProtoMapper protoMapper;
  GenericArgRepository<OprcCr> repo;
  @GrpcClient("orbit-manager")
  CrManagerGrpc.CrManagerBlockingStub orbitManager;

  @Inject
  public CrStateManager() {
    DatastoreConfRegistry registry = DatastoreConfRegistry.getDefault();
    var fac = new RepoFactory(registry.getConfMap().get("PKG"));
    repo = fac.createGenericRepo(OprcCr.class, OprcCr::getKey, "orbit");
    repo.createIfNotExist();
    crMapper = new CrMapperImpl();
  }

  public GenericArgRepository<OprcCr> getRepo() {
    return repo;
  }

  public Uni<OprcResponse> updateCr(ProtoCr protoCr) {
    var cr = crMapper.map(protoCr);
    return repo.persistAsync(cr)
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
    var cr = getRepo().get(OprcCr.toKey(cls.getStatus().getCrId()));
    if (cr == null)
      throw new StdOaasException("No matched CR for give class");
    var response = orbitManager.detach(DetachCrRequest.newBuilder()
      .setOrbit(crMapper.map(cr))
      .setCls(protoMapper.toProto(cls))
      .build());
    var newCr = response.getCr();
    if (response.getCr().getAttachedClsList().isEmpty()) {
      repo.remove(OprcCr.toKey(newCr.getId()));
    } else {
      repo.persistAsync(crMapper.map(newCr));
    }
    cls.getStatus().setCrId(0);
  }
}
