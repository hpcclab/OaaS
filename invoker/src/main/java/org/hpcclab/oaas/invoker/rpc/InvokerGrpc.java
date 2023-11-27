package org.hpcclab.oaas.invoker.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invoker.proto.*;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationStats;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.task.TaskStatus;

import java.io.IOException;

@GrpcService
public class InvokerGrpc implements InvocationService {
  final ObjectMapper mapper;
  final InvocationReqHandler invocationReqHandler;

  @Inject
  public InvokerGrpc(ObjectMapper mapper, InvocationReqHandler invocationReqHandler) {
    this.mapper = mapper;
    this.invocationReqHandler = invocationReqHandler;
  }

  @Override
  public Uni<ProtoInvocationResponse> invoke(ProtoInvocationRequest protoInvocationRequest
  ) {
    InvocationRequest req = convert(protoInvocationRequest);
    return invocationReqHandler.syncInvoke(req)
      .map(this::convert);
  }

  InvocationRequest convert(ProtoInvocationRequest request) {
    try {
      return new InvocationRequest(
        request.getMain(),
        request.getCls(),
        request.getFb(),
        DSMap.copy(request.getArgsMap()),
        request.getInputsList(),
        request.getImmutable(),
        request.getMacro(),
        request.getInMacro(),
        request.getInvId(),
        request.getOutId(),
        DSMap.copy(request.getMacroIdsMap()),
        request.getPreloadingNode(),
        request.getQueTs(),
        mapper.readValue(request.getBody().toByteArray(), ObjectNode.class),
        request.getMain()
      );
    } catch (IOException e) {
      throw new InvocationException("parsing error", e, 400);
    }
  }

  ProtoInvocationResponse convert(InvocationContext context) {
    return ProtoInvocationResponse.newBuilder()
      .setMain(convert(context.getMain()))
      .setOutput(convert(context.getOutput()))
      .setInvId(context.initNode().getKey())
      .setFb(context.getFbName())
      .setStatus(convert(context.initNode().getStatus()))
      .setBody(convert(context.getRespBody()))
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

  ProtoTaskStatus convert(TaskStatus status){
    return switch (status){
      case LAZY -> ProtoTaskStatus.LAZY;
      case DOING -> ProtoTaskStatus.DOING;
      case SUCCEEDED -> ProtoTaskStatus.SUCCEEDED;
      case FAILED -> ProtoTaskStatus.FAILED;
      case DEPENDENCY_FAILED -> ProtoTaskStatus.DEPENDENCY_FAILED;
      case READY -> ProtoTaskStatus.READY;
    };
  }
  OObject convert(OaasObject object) {
    var b = OObject.newBuilder()
      .setKey(object.getKey())
      .setCls(object.getCls())
      .setData(convert(object.getData()))
      .setLastOffset(object.getLastOffset())
      .setRevision(object.getRevision());
    var stateBuilder = OOState.newBuilder();
    if (object.getState().getOverrideUrls() != null)
      stateBuilder.putAllVerIds(object.getState().getOverrideUrls());
    if (object.getState().getVerIds() != null)
      stateBuilder.putAllVerIds(object.getState().getVerIds());
    b.setState(stateBuilder);
    if (object.getRefs() != null)
      b.putAllRefs(object.getRefs());
    return b.build();
  }

  ByteString convert(ObjectNode objectNode) {
    try {
      return ByteString.copyFrom(mapper.writeValueAsBytes(objectNode));
    } catch (JsonProcessingException e) {
      throw new InvocationException("",e);
    }
  }
}
