package org.hpcclab.oaas.invoker.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.proto.InvocationService;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class InvocationServiceImpl implements InvocationService {
  private static final Logger logger = LoggerFactory.getLogger(InvocationServiceImpl.class);
  final InvocationReqHandler invocationReqHandler;
  final ProtoObjectMapper mapper;

  @Inject
  public InvocationServiceImpl(InvocationReqHandler invocationReqHandler,
                               ProtoObjectMapper protoObjectMapper) {
    this.invocationReqHandler = invocationReqHandler;
    this.mapper = protoObjectMapper;
  }

  @Override
  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest protoInvocationRequest
  ) {
    InvocationRequest req = mapper.fromProto(protoInvocationRequest);
    return invocationReqHandler.syncInvoke(req)
      .map(mapper::toProto)
      .onFailure()
      .invoke(e -> logger.error("invoke error", e));
  }
}
