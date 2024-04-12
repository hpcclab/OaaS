package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.ClassController;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.proto.*;
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
                                    ProtoObjectMapper mapper,
                                    GrpcClient grpcClient,
                                    InvocationReqHandler invocationReqHandler,
                                    InvokerManager invokerManager) {
    this.lookupManager = lookupManager;
    this.registry = registry;
    this.mapper = mapper;
    this.grpcClient = grpcClient;
    this.invocationReqHandler = invocationReqHandler;
    this.invokerManager = invokerManager;
  }

  public static SocketAddress toSocketAddress(CrHash.ApiAddress address) {
    return SocketAddress.inetSocketAddress(address.port(), address.host());
  }


  public Uni<ProtoInvocationResponse> invoke(ProtoObjectAccessLanguage protOal) {
    boolean managed = invokerManager.getManagedCls().contains(protOal.getCls());
    if (managed && (protOal.getMain().isEmpty())) {
      return invocationReqHandler.invoke(mapper.fromProto(protOal))
        .map(mapper::toProto);
    }

    if (managed && registry.getClassController(protOal.getCls()).getCls().getConfig().isDisableHashAware()) {
      return invocationReqHandler.invoke(mapper.fromProto(protOal))
        .map(mapper::toProto);
    }
    CrHash.ApiAddress addr = resolveAddr(protOal.getCls(), protOal.getMain());
    if (addr==null || lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}/{}", protOal.getCls(), protOal.getMain(), protOal.getFb());
      return invocationReqHandler.invoke(mapper.fromProto(protOal))
        .map(mapper::toProto);
    } else {
      logger.debug("invoke remote {}~{}/{} to {}:{}", protOal.getCls(), protOal.getMain(),
        protOal.getFb(), addr.host(), addr.port());
      return send(addr, protOal);
    }
  }


  public Uni<InvocationResponse> invoke(ObjectAccessLanguage oal) {
    boolean managed = invokerManager.getManagedCls().contains(oal.getCls());
    if (managed && (oal.getMain()==null || oal.getMain().isEmpty())) {
      return invocationReqHandler.invoke(oal);
    }
    if (managed && registry.getClassController(oal.getCls()).getCls().getConfig().isDisableHashAware()) {
      return invocationReqHandler.invoke(oal);
    }
    CrHash.ApiAddress addr = resolveAddr(oal.getCls(), oal.getMain());
    if (addr==null || lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}", oal.getCls(), oal.getMain(), oal.getFb());
      return invocationReqHandler.invoke(oal);
    } else {
      logger.debug("invoke remote {}~{}:{} to {}:{}",
        oal.getCls(), oal.getMain(), oal.getFb(), addr.host(), addr.port());
      return send(addr, mapper.toProto(oal))
        .map(mapper::fromProto);
    }
  }

  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest request) {
    boolean managed = invokerManager.getManagedCls().contains(request.getCls());
    if (managed && request.getMain().isEmpty()) {
      return invocationReqHandler.invoke(mapper.fromProto(request))
        .map(mapper::toProto);
    }
    CrHash.ApiAddress addr = resolveAddr(request.getCls(), request.getMain());
    if (addr==null || lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}", request.getCls(), request.getMain(), request.getFb());
      return invocationReqHandler.invoke(mapper.fromProto(request))
        .map(mapper::toProto);
    } else {
      logger.debug("invoke remote {}~{}:{} to {}:{}",
        request.getCls(), request.getMain(), request.getFb(), addr.host(), addr.port());
      return send(addr, request);
    }
  }

  private CrHash.ApiAddress resolveAddr(String clsKey, String obj) {
    ClassController classController = registry.getClassController(clsKey);
    if (classController==null) throw StdOaasException.notFoundCls400(clsKey);
    var cls = classController.getCls();
    var lookup = lookupManager.getOrInit(cls);
    CrHash.ApiAddress addr;
    if (obj==null || obj.isEmpty()) {
      addr = lookup.getAny();
    } else {
      addr = lookup.find(obj);
    }
    return addr;
  }

  private Uni<ProtoInvocationResponse> send(CrHash.ApiAddress addr,
                                            ProtoInvocationRequest request) {
    MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeLocalMethod();
    return toUni(grpcClient.request(toSocketAddress(addr), invokeMethod))
      .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)))
      .flatMap(resp -> toUni(resp.last()));
  }

  private Uni<ProtoInvocationResponse> send(CrHash.ApiAddress addr,
                                            ProtoObjectAccessLanguage request) {
    MethodDescriptor<ProtoObjectAccessLanguage, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeOalMethod();
    return toUni(grpcClient.request(toSocketAddress(addr), invokeMethod))
      .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)))
      .flatMap(resp -> toUni(resp.last()));
  }
}
