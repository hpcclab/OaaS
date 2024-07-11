package org.hpcclab.oaas.invoker.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClosedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.LocationAwareInvocationForwarder;
import org.hpcclab.oaas.invocation.controller.ClassController;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.invoker.lookup.ObjLocalResolver;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.time.Duration;
import java.util.function.Supplier;


@ApplicationScoped
public class HashAwareInvocationHandler implements LocationAwareInvocationForwarder {
  private static final Logger logger = LoggerFactory.getLogger(HashAwareInvocationHandler.class);
  final LookupManager lookupManager;
  final ClassControllerRegistry registry;
  final InvocationReqHandler invocationReqHandler;
  final InvokerManager invokerManager;
  final InvokerConfig invokerConfig;
  final ProtoMapper mapper = new ProtoMapperImpl();
  final RemoteInvocationSender invocationSender;

  final int retry;
  final int backoff;
  final int maxBackoff;
  final boolean forceInvokeLocal;

  @Inject
  public HashAwareInvocationHandler(LookupManager lookupManager,
                                    ClassControllerRegistry registry,
                                    InvocationReqHandler invocationReqHandler,
                                    InvokerManager invokerManager,
                                    InvokerConfig invokerConfig,
                                    RemoteInvocationSender invocationSender) {
    this.lookupManager = lookupManager;
    this.registry = registry;
    this.invocationReqHandler = invocationReqHandler;
    this.invokerManager = invokerManager;
    this.invokerConfig = invokerConfig;
    this.retry = invokerConfig.syncMaxRetry();
    this.backoff = invokerConfig.syncRetryBackOff();
    this.maxBackoff = invokerConfig.syncMaxRetryBackOff();
    this.forceInvokeLocal = invokerConfig.forceInvokeLocal();
    this.invocationSender = invocationSender;
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
      return setupRetry(invocationSender.send(supplier, mapper.toProto(request)))
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
      return setupRetry(invocationSender.send(supplier, request));
    }
  }

  private ObjLocalResolver resolveAddr(String clsKey) {
    ClassController classController = registry.getClassController(clsKey);
    if (classController==null) throw StdOaasException.notFoundCls400(clsKey);
    var cls = classController.getCls();
    return lookupManager.getOrInit(cls);
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

    public RetryableException(Throwable cause) {
      super(null, cause);
    }
  }
}
