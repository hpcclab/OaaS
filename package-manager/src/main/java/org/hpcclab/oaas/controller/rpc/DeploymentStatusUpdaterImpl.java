package org.hpcclab.oaas.controller.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;

import java.util.List;

@GrpcService
public class DeploymentStatusUpdaterImpl implements DeploymentStatusUpdater {
  @Inject
  ClassRepository clsRepo;
  @Inject
  FunctionRepository fnRepo;
  @Inject
  ProtoMapper mapper;

  @Override
  public Uni<OprcResponse> updateCls(OClassStatusUpdate update) {
    var status = mapper.fromProto(update.getStatus());
    return clsRepo.async()
      .computeAsync(update.getKey(), (k,cls) -> cls.setStatus(status))
      .map(__ -> OprcResponse.newBuilder().setSuccess(true).build());
  }

  @Override
  public Uni<OprcResponse> updateFn(OFunctionStatusUpdate update) {
    var status = mapper.fromProto(update.getStatus());
    return fnRepo.async()
      .computeAsync(update.getKey(), (k,fn) -> fn.setStatus(status))
      .map(__ -> OprcResponse.newBuilder().setSuccess(true).build());
  }

  @Override
  @RunOnVirtualThread
  public Uni<OprcResponse> updateClsAll(OClassStatusUpdates request) {
    List<OClassStatusUpdate> list = request.getUpdateListList();
    var keys = list.stream().map(OClassStatusUpdate::getKey).toList();
    var clsMap = clsRepo.list(keys);
    for (OClassStatusUpdate update : list) {
      var cls = clsMap.get(update.getKey());
      if (cls == null) continue;
      cls.setStatus(mapper.fromProto(update.getStatus()));
    }
    clsRepo.persist(clsMap.values());
    return Uni.createFrom().item(OprcResponse.newBuilder().setSuccess(true).build());
  }

  @Override
  @RunOnVirtualThread
  public Uni<OprcResponse> updateFnAll(OFunctionStatusUpdates request) {
    List<OFunctionStatusUpdate> list = request.getUpdateListList();
    var keys = list.stream().map(OFunctionStatusUpdate::getKey).toList();
    var fnMap = fnRepo.list(keys);
    for (var update : list) {
      var fn = fnMap.get(update.getKey());
      if (fn == null) continue;
      fn.setStatus(mapper.fromProto(update.getStatus()));
    }
    fnRepo.persist(fnMap.values());
    return Uni.createFrom().item(OprcResponse.newBuilder().setSuccess(true).build());
  }
}
