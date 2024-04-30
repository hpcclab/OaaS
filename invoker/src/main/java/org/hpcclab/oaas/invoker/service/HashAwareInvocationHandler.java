package org.hpcclab.oaas.invoker.service;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
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
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.proto.InvocationServiceGrpc;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.time.Duration;
import java.util.function.Supplier;


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
  final GrpcInvocationServicePool pool;

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
    this.pool = new GrpcInvocationServicePool();
  }

  public static SocketAddress toSocketAddress(CrHash.ApiAddress address) {
    return SocketAddress.inetSocketAddress(address.port(), address.host());
  }


  public Uni<InvocationResponse> invoke(InvocationRequest request) {
    if (forceInvokeLocal) {
      return invocationReqHandler.invoke(request);
    }
    boolean managed = invokerManager.getManagedCls().contains(request.cls());
    if (managed && (request.main()==null || request.main().isEmpty())) {
      return invocationReqHandler.invoke(request);
    }
    ObjLocalResolver resolver = resolveAddr(request.cls());
    Supplier<CrHash.ApiAddress> supplier = resolver.createSupplier(request.main());
    CrHash.ApiAddress addr = supplier.get();
    if (lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}",
        request.cls(), request.main(), request.fb());
      return invocationReqHandler.invoke(request);
    } else {
      if (addr!=null) {
        logger.debug("invoke remote {}~{}:{} to {}:{}",
          request.cls(), request.main(), request.fb(), addr.host(), addr.port());
      } else {
        logger.debug("invoke remote {}~{}:{}",
          request.cls(), request.main(), request.fb());
      }

      return sendWithPool(supplier, mapper.toProto(request))
        .map(mapper::fromProto);
    }
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
    Supplier<CrHash.ApiAddress> supplier = resolver.createSupplier(request.getMain());
    CrHash.ApiAddress addr = supplier.get();
    if (lookupManager.isLocal(addr)) {
      logger.debug("invoke local {}~{}:{}", request.getCls(), request.getMain(), request.getFb());
      return invocationReqHandler.invoke(mapper.fromProto(request))
        .map(mapper::toProto);
    } else {
      if (addr!=null)
        logger.debug("invoke remote {}~{}:{} to {}:{}",
          request.getCls(), request.getMain(), request.getFb(), addr.host(), addr.port());
      else {
        logger.debug("invoke remote {}~{}:{}",
          request.getCls(), request.getMain(), request.getFb());
      }
      return sendWithPool(supplier, request);
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
    Uni<ProtoInvocationResponse> clientResponseUni =
      Uni.createFrom().item(addrSupplier)
        .onItem().ifNull().failWith(RetryableException::new)
        .flatMap(addr -> callGrpc(request, addr)
        );
    return setupRetry(clientResponseUni);
  }

  private Uni<ProtoInvocationResponse> sendWithPool(Supplier<CrHash.ApiAddress> addrSupplier,
                                                    ProtoInvocationRequest request) {
    Uni<ProtoInvocationResponse> clientResponseUni =
      Uni.createFrom().item(addrSupplier)
        .onItem().ifNull().failWith(RetryableException::new)
        .map(pool::getOrCreate)
        .flatMap(invocationService -> invocationService.invokeLocal(request));
    return setupRetry(clientResponseUni);
  }

  private Uni<ProtoInvocationResponse> callGrpc(ProtoInvocationRequest request,
                                                CrHash.ApiAddress addr) {
    MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod =
      InvocationServiceGrpc.getInvokeLocalMethod();
    return Uni.createFrom().emitter(emitter ->
      grpcClient.request(toSocketAddress(addr), invokeMethod)
        .compose(grpcClientRequest -> {
          logger.debug("sending request to {}", addr);
          if (grpcClientRequest.writeQueueFull())
            logger.debug("writeQueueFull to {}", addr);
          return grpcClientRequest
            .exceptionHandler(emitter::fail)
            .end(request)
            .compose(__ -> {
              logger.debug("done end to {}", addr);
              return grpcClientRequest.response();
            });
        })
        .compose(resp -> {
          logger.debug("handling resp from {}", addr);
          if (resp.status()==GrpcStatus.UNAVAILABLE ||
            resp.status()==GrpcStatus.UNKNOWN)
            return Future.failedFuture(new RetryableException());
          resp.errorHandler(err -> emitter.fail(StdOaasException.format("grpc error %s", err.toString())));
          resp.exceptionHandler(err -> emitter.fail(new StdOaasException("grpc error", err)));
          return resp.last();
        })
        .onSuccess(emitter::complete)
        .onFailure(emitter::fail)
    );

  }

  Uni<ProtoInvocationResponse> setupRetry(Uni<ProtoInvocationResponse> uni) {
    if (retry <= 0) {
      return uni;
    }
    Uni<ProtoInvocationResponse> invocationResponseUni = uni
      .onFailure(StatusRuntimeException.class)
      .transform(err -> {
        if (err instanceof StatusRuntimeException statusRuntimeException) {
          Status.Code code = statusRuntimeException.getStatus().getCode();
          if (code==Status.Code.UNAVAILABLE || code==Status.Code.UNKNOWN)
            return new RetryableException();
        }
        return err;
      })
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
