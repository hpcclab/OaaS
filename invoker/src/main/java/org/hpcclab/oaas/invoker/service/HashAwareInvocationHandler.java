package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.ClassController;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.invoker.lookup.ObjLocalResolver;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.proto.InvocationServiceGrpc;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.hpcclab.oaas.proto.ProtoObjectAccessLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.time.Duration;
import java.util.function.Supplier;

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
  final InvokerConfig invokerConfig;

  final int retry;
  final int backoff;
  final int maxBackoff;
  final boolean forceInvokeLocal;

  @Inject
  public HashAwareInvocationHandler(LookupManager lookupManager,
                                    ClassControllerRegistry registry,
                                    ProtoObjectMapper mapper,
                                    GrpcClient grpcClient,
                                    InvocationReqHandler invocationReqHandler,
                                    InvokerManager invokerManager, InvokerConfig invokerConfig) {
    this.lookupManager = lookupManager;
    this.registry = registry;
    this.mapper = mapper;
    this.grpcClient = grpcClient;
    this.invocationReqHandler = invocationReqHandler;
    this.invokerManager = invokerManager;
    this.invokerConfig = invokerConfig;
    this.retry = invokerConfig.syncMaxRetry();
    this.backoff = invokerConfig.syncRetryBackOff();
    this.maxBackoff = invokerConfig.syncMaxRetryBackOff();
    this.forceInvokeLocal = invokerConfig.forceInvokeLocal();
  }

  public static SocketAddress toSocketAddress(CrHash.ApiAddress address) {
    return SocketAddress.inetSocketAddress(address.port(), address.host());
  }


  public Uni<ProtoInvocationResponse> invoke(ProtoObjectAccessLanguage protoOal) {
    if (forceInvokeLocal) {
      return invocationReqHandler.invoke(mapper.fromProto(protoOal))
        .map(mapper::toProto);
    }
    boolean managed = invokerManager.getManagedCls().contains(protoOal.getCls());
    if (managed && (protoOal.getMain().isEmpty())) {
      return invocationReqHandler.invoke(mapper.fromProto(protoOal))
        .map(mapper::toProto);
    }
//    if (managed && registry.getClassController(protoOal.getCls()).getCls().getConfig().isDisableHashAware()) {
//      return invocationReqHandler.invoke(mapper.fromProto(protoOal))
//        .map(mapper::toProto);
//    }
    ObjLocalResolver resolver = resolveAddr(protoOal.getCls());
    CrHash.ApiAddress addr = resolver.find(protoOal.getMain());
    if (lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}/{}", protoOal.getCls(), protoOal.getMain(), protoOal.getFb());
      return invocationReqHandler.invoke(mapper.fromProto(protoOal))
        .map(mapper::toProto);
    } else {
      logger.debug("invoke remote {}~{}/{} to {}:{}", protoOal.getCls(), protoOal.getMain(),
        protoOal.getFb(), addr.host(), addr.port());
      return send(() -> resolver.find(protoOal.getMain()), protoOal);
    }
  }


  public Uni<InvocationResponse> invoke(ObjectAccessLanguage oal) {
    return invoke(mapper.toProto(oal))
      .map(mapper::fromProto);
  }

  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest request) {
    if (forceInvokeLocal) {
      return invocationReqHandler.invoke(mapper.fromProto(request))
        .map(mapper::toProto);
    }
    boolean managed = invokerManager.getManagedCls().contains(request.getCls());
    if (managed && request.getMain().isEmpty()) {
      return invocationReqHandler.invoke(mapper.fromProto(request))
        .map(mapper::toProto);
    }
    ObjLocalResolver resolver = resolveAddr(request.getCls());
    CrHash.ApiAddress addr = resolver.find(request.getMain());
    if (lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}", request.getCls(), request.getMain(), request.getFb());
      return invocationReqHandler.invoke(mapper.fromProto(request))
        .map(mapper::toProto);
    } else {
      logger.debug("invoke remote {}~{}:{} to {}:{}",
        request.getCls(), request.getMain(), request.getFb(), addr.host(), addr.port());
      return send(() -> resolver.find(request.getMain()), request);
    }
  }

  private ObjLocalResolver resolveAddr(String clsKey) {
    ClassController classController = registry.getClassController(clsKey);
    if (classController==null) throw StdOaasException.notFoundCls400(clsKey);
    var cls = classController.getCls();
    return lookupManager.getOrInit(cls);
  }

  private Uni<ProtoInvocationResponse> send(Supplier<CrHash.ApiAddress> addrSupplier,
                                            ProtoInvocationRequest request) {
    MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeLocalMethod();
    Uni<GrpcClientResponse<ProtoInvocationRequest, ProtoInvocationResponse>> clientResponseUni =
      Uni.createFrom().item(addrSupplier)
        .onItem().ifNull().failWith(RetryableException::new)
        .flatMap(addr -> toUni(grpcClient.request(toSocketAddress(addr), invokeMethod)))
        .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)));

    return setupRetry(clientResponseUni);
  }

  private Uni<ProtoInvocationResponse> send(Supplier<CrHash.ApiAddress> addrSupplier,
                                            ProtoObjectAccessLanguage request) {
    MethodDescriptor<ProtoObjectAccessLanguage, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeOalMethod();
    Uni<GrpcClientResponse<ProtoObjectAccessLanguage, ProtoInvocationResponse>> uni =
      Uni.createFrom().item(addrSupplier)
        .onItem().ifNull().failWith(RetryableException::new)
        .flatMap(addr -> toUni(grpcClient.request(toSocketAddress(addr), invokeMethod)))
        .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)));
    return setupRetry(uni);
  }

  <T> Uni<ProtoInvocationResponse> setupRetry(Uni<GrpcClientResponse<T, ProtoInvocationResponse>> uni) {
    Uni<ProtoInvocationResponse> responseUni = uni
      .flatMap(resp -> {
        if (resp.status()==GrpcStatus.UNAVAILABLE ||
          resp.status()==GrpcStatus.UNKNOWN)
          return Uni.createFrom().failure(new RetryableException());
        return toUni(resp.last());
      });
    if (retry <= 0) {
      return responseUni;
    }
    Uni<ProtoInvocationResponse> invocationResponseUni = responseUni
      .onFailure(throwable -> throwable instanceof RetryableException ||
        throwable instanceof ConnectException ||
        throwable instanceof HttpClosedException
      )
      .retry()
      .withBackOff(Duration.ofMillis(backoff), Duration.ofMillis(maxBackoff))
      .atMost(retry);
    if (logger.isDebugEnabled()) {
      invocationResponseUni = invocationResponseUni
        .onFailure()
        .invoke(throwable -> logger.debug("unexpected exception", throwable));
    }
    return invocationResponseUni;
  }

  static class RetryableException extends StdOaasException {
    public RetryableException() {
      super(502);
    }
  }
}
