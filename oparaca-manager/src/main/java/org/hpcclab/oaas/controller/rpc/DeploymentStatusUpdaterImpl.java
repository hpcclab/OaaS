package org.hpcclab.oaas.controller.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;

@ApplicationScoped
@GrpcService
public class DeploymentStatusUpdaterImpl implements DeploymentStatusUpdater {
  @Inject
  ClassRepository clsRepo;
  @Inject
  FunctionRepository fnRepo;
  @Inject
  ProtoMapper mapper;

  @Override
  public Uni<ProtoOClass> updateCls(OClassStatusUpdate update) {
    var status = mapper.fromProto(update.getStatus());
    return clsRepo.async()
      .computeAsync(update.getKey(), (k,cls) -> cls.setStatus(status))
      .map(mapper::toProto);
  }

  @Override
  public Uni<ProtoOFunction> updateFn(OFunctionStatusUpdate update) {
    var status = mapper.fromProto(update.getStatus());
    return fnRepo.async()
      .computeAsync(update.getKey(), (k,fn) -> fn.setStatus(status))
      .map(mapper::toProto);
  }

}
