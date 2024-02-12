package org.hpcclab.oaas.invoker.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.RouterInvocationReqHandler;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationStats;

@GrpcService
public class InvocationServiceImpl implements InvocationService {
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
      .map(mapper::toProto);
  }
}
