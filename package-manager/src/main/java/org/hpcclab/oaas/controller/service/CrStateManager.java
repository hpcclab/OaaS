package org.hpcclab.oaas.controller.service;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.hpcclab.oaas.arango.RepoFactory;
import org.hpcclab.oaas.arango.repo.GenericArgRepository;
import org.hpcclab.oaas.controller.model.CrHash;
import org.hpcclab.oaas.controller.model.CrMapper;
import org.hpcclab.oaas.controller.model.CrMapperImpl;
import org.hpcclab.oaas.controller.model.OprcCr;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@ApplicationScoped
public class CrStateManager {
  private static final Logger logger = LoggerFactory.getLogger(CrStateManager.class);
  @Inject
  CrMapper crMapper;
  @Inject
  ProtoMapper protoMapper;
  GenericArgRepository<OprcCr> crRepo;
  GenericArgRepository<CrHash> hashRepo;
  @GrpcClient("orbit-manager")
  CrManagerGrpc.CrManagerBlockingStub orbitManager;
  @Channel("crHashs")
  MutinyEmitter<Record<String, Buffer>> crHashEmitter;

  @Inject
  public CrStateManager() {
    DatastoreConfRegistry registry = DatastoreConfRegistry.getDefault();
    var fac = new RepoFactory(registry.getConfMap().get("PKG"));
    crRepo = fac.createGenericRepo(OprcCr.class, OprcCr::getKey, "cr");
    crRepo.createIfNotExist();
    hashRepo = fac.createGenericRepo(CrHash.class, CrHash::getKey, "crHash");
    hashRepo.createIfNotExist();
    crMapper = new CrMapperImpl();
  }

  public GenericArgRepository<OprcCr> getCrRepo() {
    return crRepo;
  }

  public Uni<OprcResponse> updateCr(ProtoCr protoCr) {
    var cr = crMapper.fromProto(protoCr);
    return crRepo.persistAsync(cr)
      .map(entity -> OprcResponse.newBuilder()
        .setSuccess(true)
        .build());
  }

  public Uni<ProtoCrHash> updateCrHash(ProtoCrHash protoCrHash) {
    var crHash = crMapper.fromProto(protoCrHash);
    return hashRepo.computeAsync(crHash.getKey(), (key, entity) -> {
        if (entity==null) return crHash;
        return CrHash.merge(entity, crHash);
      })
      .call(hash -> crHashEmitter.send(Record.of(
        hash.getKey(),
        Buffer.buffer(crMapper.toProto(hash).toByteArray()))
      ))
      .map(crMapper::toProto);
  }

  public Uni<ProtoCr> get(String id) {
    return crRepo.getAsync(id)
      .map(doc -> crMapper.toProto(doc));
  }

  public Uni<ProtoCrHash> getHash(String id) {
    return hashRepo.getAsync(id)
      .map(doc -> crMapper.toProto(doc));
  }

  public Multi<ProtoCr> listOrbit(PaginateQuery request) {
    return crRepo.getQueryService()
      .paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(p -> Multi.createFrom().iterable(p.getItems()))
      .map(doc -> crMapper.toProto(doc));
  }
  public Multi<ProtoCrHash> listHash(PaginateQuery request) {
    return hashRepo.getQueryService()
      .paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(p -> Multi.createFrom().iterable(p.getItems()))
      .map(doc -> crMapper.toProto(doc));
  }

  public void detach(OClass cls) {
    var cr = getCrRepo().get(OprcCr.toKey(cls.getStatus().getCrId()));
    if (cr==null)
      throw new StdOaasException("No matched CR for give class");
    var response = orbitManager.detach(DetachCrRequest.newBuilder()
      .setOrbit(crMapper.toProto(cr))
      .setCls(protoMapper.toProto(cls))
      .build());
    var newCr = response.getCr();
    if (response.getCr().getAttachedClsList().isEmpty()) {
      crRepo.remove(OprcCr.toKey(newCr.getId()));
    } else {
      crRepo.persistAsync(crMapper.fromProto(newCr));
    }
    cls.getStatus().setCrId(0);
  }
}
