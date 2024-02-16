package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.smallrye.mutiny.vertx.UniHelper.toUni;


@ApplicationScoped
public class HashAwareInvocationHandler {
  private static final Logger logger = LoggerFactory.getLogger(HashAwareInvocationHandler.class);
  final LookupManager lookupManager;
  final ClassControllerRegistry registry;
  final ProtoObjectMapper mapper;
  final GrpcClient grpcClient;
  final InvocationReqHandler invocationReqHandler;
  final InvokerManager invokerManager;

  @Inject
  public HashAwareInvocationHandler(LookupManager lookupManager,
                                    ClassControllerRegistry registry,
                                    Vertx vertx,
                                    ProtoObjectMapper mapper,
                                    InvocationReqHandler invocationReqHandler,
                                    InvokerManager invokerManager) {
    this.lookupManager = lookupManager;
    this.registry = registry;
    this.mapper = mapper;
    this.invocationReqHandler = invocationReqHandler;
    this.grpcClient = GrpcClient.client(vertx);
    this.invokerManager = invokerManager;
  }

  public static SocketAddress toSocketAddress(ProtoApiAddress address) {
    return SocketAddress.inetSocketAddress(address.getPort(), address.getHost());
  }


  public Uni<ProtoInvocationResponse> invoke(ProtoObjectAccessLanguage protOal) {
    boolean managed = invokerManager.getManagedCls().contains(protOal.getCls());
    if (managed && (protOal.getMain().isEmpty())) {
      return invocationReqHandler.syncInvoke(mapper.fromProto(protOal))
        .map(mapper::toProto);
    }
    ProtoApiAddress addr = resolveAddr(protOal.getCls(), protOal.getMain());
    if (addr==null || lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}/{}", protOal.getCls(), protOal.getMain(), protOal.getFb());
      return invocationReqHandler.syncInvoke(mapper.fromProto(protOal))
        .map(mapper::toProto);
    } else {
      logger.debug("invoke remote {}~{}/{} to {}", protOal.getCls(), protOal.getMain(),
        protOal.getFb(), addr);
      return send(addr, protOal);
    }
  }


  public Uni<InvocationResponse> invoke(ObjectAccessLanguage oal) {
    boolean managed = invokerManager.getManagedCls().contains(oal.getCls());
    if (managed && (oal.getMain()==null || oal.getMain().isEmpty())) {
      return invocationReqHandler.syncInvoke(oal);
    }
    ProtoApiAddress addr = resolveAddr(oal.getCls(), oal.getMain());
    if (addr==null || lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}", oal.getCls(), oal.getMain(), oal.getFb());
      return invocationReqHandler.syncInvoke(oal);
    } else {
      logger.debug("invoke remote {}~{}:{}", oal.getCls(), oal.getMain(), oal.getFb());
      return send(addr, mapper.toProto(oal))
        .map(mapper::fromProto);
    }
  }

  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest request) {
    boolean managed = invokerManager.getManagedCls().contains(request.getCls());
    if (managed && request.getMain().isEmpty()) {
      return invocationReqHandler.syncInvoke(mapper.fromProto(request))
        .map(mapper::toProto);
    }
    ProtoApiAddress addr = resolveAddr(request.getCls(), request.getMain());
    if (addr==null || lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}", request.getCls(), request.getMain(), request.getFb());
      return invocationReqHandler.syncInvoke(mapper.fromProto(request))
        .map(mapper::toProto);
    } else {
      logger.debug("invoke remote {}~{}:{}", request.getCls(), request.getMain(), request.getFb());
      return send(addr, request);
    }
  }

  private ProtoApiAddress resolveAddr(String protOal, String obj) {
    var cls = registry.getClassController(protOal).getCls();
    var lookup = lookupManager.getOrInit(cls);
    ProtoApiAddress addr;
    if (obj==null || obj.isEmpty()) {
      addr = lookup.getAny();
    } else {
      addr = lookup.find(obj);
    }
    return addr;
  }

  private Uni<ProtoInvocationResponse> send(ProtoApiAddress addr,
                                            ProtoInvocationRequest request) {
    MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeLocalMethod();
    return toUni(grpcClient.request(toSocketAddress(addr), invokeMethod))
      .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)))
      .flatMap(resp -> toUni(resp.last()));
  }

  private Uni<ProtoInvocationResponse> send(ProtoApiAddress addr,
                                            ProtoObjectAccessLanguage request) {
    MethodDescriptor<ProtoObjectAccessLanguage, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeOalMethod();
    return toUni(grpcClient.request(toSocketAddress(addr), invokeMethod))
      .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)))
      .flatMap(resp -> toUni(resp.last()));
  }
}
