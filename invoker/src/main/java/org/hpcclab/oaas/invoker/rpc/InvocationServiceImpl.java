package org.hpcclab.oaas.invoker.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.proto.InvocationService;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.hpcclab.oaas.proto.ProtoObjectAccessLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class InvocationServiceImpl implements InvocationService {
  private static final Logger logger = LoggerFactory.getLogger(InvocationServiceImpl.class);
  final InvocationReqHandler invocationReqHandler;
  final HashAwareInvocationHandler hashAwareInvocationHandler;
  final ProtoObjectMapper mapper;

  @Inject
  public InvocationServiceImpl(InvocationReqHandler invocationReqHandler,
                               HashAwareInvocationHandler hashAwareInvocationHandler,
                               ProtoObjectMapper protoObjectMapper) {
    this.invocationReqHandler = invocationReqHandler;
    this.hashAwareInvocationHandler = hashAwareInvocationHandler;
    this.mapper = protoObjectMapper;
  }


  @Override
  public Uni<ProtoInvocationResponse> invokeLocal(ProtoInvocationRequest protoInvocationRequest
  ) {
    InvocationRequest req = mapper.fromProto(protoInvocationRequest);
    return invocationReqHandler.syncInvoke(req)
      .map(mapper::toProto)
      .onFailure()
      .invoke(e -> logger.error("invokeLocal error", e));
  }

  @Override
  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest protoInvocationRequest
  ) {
    return hashAwareInvocationHandler.invoke(protoInvocationRequest)
      .onFailure()
      .invoke(e -> logger.error("invoke error", e));
  }

  @Override
  public Uni<ProtoInvocationResponse> invokeOal(ProtoObjectAccessLanguage request) {
    return hashAwareInvocationHandler.invoke(request)
      .onFailure()
      .invoke(e -> logger.error("invokeOal error", e));
  }
}
