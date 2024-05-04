package org.hpcclab.oaas.invoker.rpc;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.lookup.LookupManager;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.exception.TooManyRequestException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.proto.InvocationService;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class InvokerInvocationService implements InvocationService {
  private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationService.class);
  final InvocationReqHandler invocationReqHandler;
  final HashAwareInvocationHandler hashAwareInvocationHandler;
  final ProtoObjectMapper mapper;
  final LookupManager lookupManager;
  final ClassControllerRegistry registry;

  @Inject
  public InvokerInvocationService(InvocationReqHandler invocationReqHandler,
                                  HashAwareInvocationHandler hashAwareInvocationHandler,
                                  ProtoObjectMapper protoObjectMapper, LookupManager lookupManager, ClassControllerRegistry registry) {
    this.invocationReqHandler = invocationReqHandler;
    this.hashAwareInvocationHandler = hashAwareInvocationHandler;
    this.mapper = protoObjectMapper;
    this.lookupManager = lookupManager;
    this.registry = registry;
  }


  @Override
  public Uni<ProtoInvocationResponse> invokeLocal(ProtoInvocationRequest protoInvocationRequest
  ) {
    logger.debug("invokeLocal {}~{}", protoInvocationRequest.getCls(), protoInvocationRequest.getMain());
    InvocationRequest req = mapper.fromProto(protoInvocationRequest);
    if (logger.isDebugEnabled() && req.cls()!=null && req.main()!=null) {
      var cls = registry.getClassController(req.cls());
      if (cls!=null) {
        CrHash.ApiAddress addr = lookupManager.getOrInit(cls.getCls())
          .find(req.main());
        var local = lookupManager.isLocal(addr);
        if (local)
          logger.debug("invoke {}~{} is local", req.cls(), req.main());
        else
          logger.debug("invoke {}~{} is not local", req.cls(), req.main());
      }
    }
    try {
      return invocationReqHandler.invoke(req)
        .map(mapper::toProto)
        .onFailure()
        .transform(this::mappingException);
    } catch (Throwable t) {
      throw mappingException(t);
    }
  }

  StatusRuntimeException mappingException(Throwable throwable) {
    if (throwable instanceof TooManyRequestException) {
      return StatusProto.toStatusRuntimeException(Status.newBuilder()
        .setCode(Code.RESOURCE_EXHAUSTED_VALUE)
        .build());
    } else if (throwable instanceof StdOaasException oaasException) {
      var code = Code.INTERNAL_VALUE;
      if (oaasException.getCode()==400) {
        code = Code.INVALID_ARGUMENT_VALUE;
      } else if (oaasException.getCode()==409) {
        code = Code.ABORTED_VALUE;
      } else if (oaasException.getCode()==501) {
        code = Code.UNIMPLEMENTED_VALUE;
      }
      return StatusProto.toStatusRuntimeException(Status.newBuilder()
        .setCode(code)
        .setMessage(oaasException.getMessage())
        .build());
    } else {
      logger.error("invoke error", throwable);
      return StatusProto.toStatusRuntimeException(Status.newBuilder()
        .setCode(Code.INTERNAL_VALUE)
        .setMessage(throwable.getMessage())
        .build());
    }
  }

  @Override
  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest protoInvocationRequest
  ) {
    return hashAwareInvocationHandler.invoke(protoInvocationRequest)
      .onFailure()
      .transform(this::mappingException);
  }

}
