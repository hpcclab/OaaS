package org.hpcclab.oaas.controller.service;

import com.arangodb.ArangoDBException;
import com.github.f4b6a3.tsid.Tsid;
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
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@ApplicationScoped
public class CrStateManager {
  private static final Logger logger = LoggerFactory.getLogger(CrStateManager.class);
  final ClassRepository clsRepo;
  final FunctionRepository fnRepo;
  CrMapper crMapper = new CrMapperImpl();
  ProtoMapper protoMapper = new ProtoMapperImpl();
  GenericArgRepository<OprcCr> crRepo;
  GenericArgRepository<CrHash> hashRepo;
  @GrpcClient("orbit-manager")
  CrManagerGrpc.CrManagerBlockingStub crManager;
  @Channel("crHashs")
  MutinyEmitter<Record<String, Buffer>> crHashEmitter;

  @Inject
  public CrStateManager(ClassRepository clsRepo, FunctionRepository fnRepo) {
    this.clsRepo = clsRepo;
    this.fnRepo = fnRepo;
    DatastoreConfRegistry registry = DatastoreConfRegistry.getDefault();
    var fac = new RepoFactory(registry.getConfMap().get("PKG"));
    crRepo = fac.createGenericRepo(OprcCr.class, OprcCr::getKey, "cr");
    crRepo.createIfNotExist();
    hashRepo = fac.createGenericRepo(CrHash.class, CrHash::getKey, "crHash");
    hashRepo.createIfNotExist();
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
    return hashRepo.async().getAsync(crHash.getKey())
      .onFailure(e -> e instanceof ArangoDBException arangoDBException
        && arangoDBException.getResponseCode()==404)
      .recoverWithNull()
      .map(entity -> {
        if (entity==null) return crHash;
        return CrHash.merge(entity, crHash);
      })
      .call(h -> hashRepo.persistAsync(h))
      .call(hash -> crHashEmitter.send(Record.of(
        hash.getKey(),
        Buffer.buffer(crMapper.toProto(hash).toByteArray()))
      ))
      .map(crMapper::toProto);
  }

  public Uni<ProtoCr> get(String id) {
    return crRepo.async().getAsync(id)
      .flatMap(this::refreshFn)
      .map(crMapper::toProto);
  }

  public Uni<OprcCr> refreshFn(OprcCr cr) {
    if (cr == null) return Uni.createFrom().nullItem();
    Set<String> fnKeys = cr.attachedFn() == null ? Set.of(): cr.attachedFn()
      .stream().map(OFunction::getKey)
      .collect(Collectors.toSet());
    return fnRepo.async().listAsync(fnKeys)
      .map(map -> cr.toBuilder()
        .attachedFn(List.copyOf(map.values()))
        .build());
  }

  public Uni<ProtoCrHash> getHash(String id) {
    return hashRepo.getAsync(id)
      .map(doc -> crMapper.toProto(doc));
  }

  public Multi<ProtoCr> listCr(PaginateQuery request) {
    return crRepo.getQueryService()
      .paginationAsync(request.getOffset(), request.getLimit())
      .toMulti()
      .flatMap(p -> Multi.createFrom().iterable(p.getItems()))
      .onItem().transformToUniAndConcatenate(this::refreshFn)
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
    var response = crManager.detach(DetachCrRequest.newBuilder()
      .setOrbit(crMapper.toProto(cr))
      .setCls(protoMapper.toProto(cls))
      .build());
    var newCr = response.getCr();
    if (response.getCr().getAttachedClsList().isEmpty()) {
      crRepo.remove(OprcCr.toKey(newCr.getId()));
      hashRepo.remove(cls.getKey());
    } else {
      crRepo.persistAsync(crMapper.fromProto(newCr));
    }
    cls.getStatus().setCrId(0);
  }

  public CrOperationResponse deploy(DeploymentUnit unit) {
    var cls = unit.getCls();
    var orbitId = cls.getStatus().getCrId();
    if (orbitId==0) {
      logger.info("deploy a new orbit for cls [{}]", cls.getKey());
      var response = crManager.deploy(unit);
      updateCr(response.getCr()).await().indefinitely();
      return response;
    } else {
      logger.info("update orbit [{}] for cls [{}]", orbitId, cls.getKey());
      var orbit = get(Tsid.from(orbitId).toLowerCase())
        .await().indefinitely();
      var req = CrUpdateRequest.newBuilder()
        .setOrbit(orbit)
        .setUnit(unit)
        .build();
      var response = crManager.update(req);
      updateCr(response.getCr())
        .await().indefinitely();
      return response;
    }
  }
}
