package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.proto.InvocationServiceGrpc;
import org.hpcclab.oaas.proto.ProtoApiAddress;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.smallrye.mutiny.vertx.UniHelper.toUni;

public class HashAwareInvocationHandler {
  private static final Logger logger = LoggerFactory.getLogger( HashAwareInvocationHandler.class );
  final LookupManager lookupManager;
  final ClassControllerRegistry registry;
  final ProtoObjectMapper mapper;
  final GrpcClient grpcClient;
  final InvocationReqHandler invocationReqHandler;
  final IdGenerator idGenerator;

  public HashAwareInvocationHandler(LookupManager lookupManager, ClassControllerRegistry registry,
                                    Vertx vertx,
                                    ProtoObjectMapper mapper,
                                    InvocationReqHandler invocationReqHandler,
                                    IdGenerator idGenerator) {
    this.lookupManager = lookupManager;
    this.registry = registry;
    this.mapper = mapper;
    this.invocationReqHandler = invocationReqHandler;
    this.grpcClient = GrpcClient.client(vertx);
    this.idGenerator = idGenerator;
  }

  public Uni<InvocationResponse> invoke(ObjectAccessLanguage oal) {
    if (oal.getMain()==null)
      return invocationReqHandler.syncInvoke(oal);

    var cls = registry.getClassController(oal.getCls()).getCls();
    var lookup = lookupManager.getOrInit(cls);
    var addr = lookup.find(oal.getMain());
    if (addr==null || lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}", oal.getCls(),oal.getMain(), oal.getFb());
      return invocationReqHandler.syncInvoke(oal);
    } else {
      logger.debug("invoke remote {}~{}:{}", oal.getCls(),oal.getMain(), oal.getFb());
      ProtoInvocationRequest request = convert(oal);
      MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod = InvocationServiceGrpc.getInvokeMethod();
      return toUni(grpcClient.request(toSocketAddress(addr), invokeMethod))
        .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)))
        .flatMap(resp -> toUni(resp.last()))
        .map(mapper::fromProto);
    }
  }

  public SocketAddress toSocketAddress(ProtoApiAddress address) {
    return SocketAddress.inetSocketAddress(address.getPort(), address.getHost());
  }

  ProtoInvocationRequest convert(ObjectAccessLanguage oal) {
    return ProtoInvocationRequest.newBuilder()
      .setMain(oal.getMain())
      .setCls(oal.getCls())
      .setFb(oal.getFb())
      .putAllArgs(oal.getArgs())
      .addAllInputs(oal.getInputs())
      .setBody(mapper.convert(oal.getBody()))
      .setInvId(idGenerator.generate())
      .build();
  }
}
