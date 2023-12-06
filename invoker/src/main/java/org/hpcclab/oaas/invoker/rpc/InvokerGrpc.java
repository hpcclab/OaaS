package org.hpcclab.oaas.invoker.rpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationStats;

@GrpcService
public class InvokerGrpc implements InvocationService {
  final InvocationReqHandler invocationReqHandler;
  final ProtoObjectMapper mapper;

  @Inject
  public InvokerGrpc(InvocationReqHandler invocationReqHandler,
                     ProtoObjectMapper protoObjectMapper) {
    this.invocationReqHandler = invocationReqHandler;
    this.mapper = protoObjectMapper;
  }

  @Override
  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest protoInvocationRequest
  ) {
    InvocationRequest req = mapper.fromProto(protoInvocationRequest);
    return invocationReqHandler.syncInvoke(req)
      .map(this::convert);
  }

  ProtoInvocationResponse convert(InvocationContext context) {
    return ProtoInvocationResponse.newBuilder()
      .setMain(mapper.toProto(context.getMain()))
      .setOutput(mapper.toProto(context.getOutput()))
      .setInvId(context.initNode().getKey())
      .setFb(context.getFbName())
      .setStatus(mapper.convert(context.initNode().getStatus()))
      .setBody(mapper.convert(context.getRespBody()))
      .setStats(convert(context.initNode().extractStats()))
      .build();
  }

  ProtoInvocationStats convert(InvocationStats stats) {
    return ProtoInvocationStats.newBuilder()
      .setCptTs(stats.getCptTs())
      .setQueTs(stats.getQueTs())
      .setSmtTs(stats.getSmtTs())
      .build();
  }



}
