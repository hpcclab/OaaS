package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.GrpcClient;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invoker.ispn.lookup.LookupManager;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.proto.InvocationServiceGrpc;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.hpcclab.oaas.repository.ClassRepository;

import static io.smallrye.mutiny.vertx.UniHelper.toUni;

public class HashAwareInvocationHandler {
  final LookupManager lookupManager;
  final ClassRepository classRepository;
  final Vertx vertx;
  final ProtoObjectMapper mapper;
  final GrpcClient grpcClient;
  final InvocationReqHandler invocationReqHandler;

  public HashAwareInvocationHandler(LookupManager lookupManager,
                                    ClassRepository classRepository,
                                    Vertx vertx,
                                    ProtoObjectMapper mapper,
                                    InvocationReqHandler invocationReqHandler) {
    this.lookupManager = lookupManager;
    this.classRepository = classRepository;
    this.vertx = vertx;
    this.mapper = mapper;
    this.invocationReqHandler = invocationReqHandler;
    grpcClient = GrpcClient.client(vertx);

  }

  public Uni<InvocationResponse> invoke(ObjectAccessLanguage oal) {
    if (oal.getMain()==null)
      return invocationReqHandler.syncInvoke(oal);

    var cls = classRepository.get(oal.getCls());
    var lookup = lookupManager.getOrInit(cls);
    var addr = lookup.find(oal.getMain());
    if (lookupManager.isLocal(addr)) {
      return invocationReqHandler.syncInvoke(oal);
    } else {
      ProtoInvocationRequest request = convert(oal);
      MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod = InvocationServiceGrpc.getInvokeMethod();
      return toUni(grpcClient.request(addr.toSocketAddress(), invokeMethod))
        .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)))
        .flatMap(resp -> toUni(resp.last()))
        .map(mapper::fromProto);
    }
  }

  ProtoInvocationRequest convert(ObjectAccessLanguage oal) {
    return ProtoInvocationRequest.newBuilder()
      .setMain(oal.getMain())
      .setCls(oal.getCls())
      .setFb(oal.getFb())
      .putAllArgs(oal.getArgs())
      .addAllInputs(oal.getInputs())
      .setBody(mapper.convert(oal.getBody()))
      .setInvId(invocationReqHandler.newId())
      .build();
  }
}
