package org.hpcclab.oaas.invoker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.GrpcClient;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invoker.ispn.lookup.LookupManager;
import org.hpcclab.oaas.invoker.proto.*;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationStats;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.repository.ClassRepository;

import java.io.IOException;

import static io.smallrye.mutiny.vertx.UniHelper.toUni;

public class HashAwareInvocationHandler {
  final LookupManager lookupManager;
  final ClassRepository classRepository;
  final Vertx vertx;
  final ObjectMapper objectMapper;
  final GrpcClient grpcClient;
  final InvocationReqHandler invocationReqHandler;

  public HashAwareInvocationHandler(LookupManager lookupManager, ClassRepository classRepository, Vertx vertx, ObjectMapper objectMapper,
                                    InvocationReqHandler invocationReqHandler) {
    this.lookupManager = lookupManager;
    this.classRepository = classRepository;
    this.vertx = vertx;
    this.objectMapper = objectMapper;
    this.invocationReqHandler = invocationReqHandler;
    grpcClient = GrpcClient.client(vertx);

  }

  public Uni<InvocationResponse> invoke(ObjectAccessLanguage oal) {
    if (oal.getMain()==null)
      return invocationReqHandler.syncInvoke(oal);

    var cls = classRepository.get(oal.getCls());
    var lookup = lookupManager.getOrInit(cls);
    var addr = lookup.resolve(oal.getMain());
    if (lookupManager.isLocal(addr)) {
      return invocationReqHandler.syncInvoke(oal);
    } else {
      ProtoInvocationRequest request = convert(oal);
      MethodDescriptor<ProtoInvocationRequest, ProtoInvocationResponse> invokeMethod = InvocationServiceGrpc.getInvokeMethod();
      return toUni(grpcClient.request(addr.toSocketAddress(), invokeMethod))
        .flatMap(grpcClientRequest -> toUni(grpcClientRequest.send(request)))
        .flatMap(resp -> toUni(resp.last()))
        .map(this::convert);
    }
  }

  ProtoInvocationRequest convert(ObjectAccessLanguage oal) {
    try {
      return ProtoInvocationRequest.newBuilder()
        .setMain(oal.getMain())
        .setCls(oal.getCls())
        .setFb(oal.getFb())
        .putAllArgs(oal.getArgs())
        .addAllInputs(oal.getInputs())
        .setBody(ByteString.copyFrom(objectMapper.writeValueAsBytes(oal.getBody())))
        .setInvId(invocationReqHandler.newId())
        .build();
    } catch (JsonProcessingException e) {
      throw new InvocationException("", e);
    }
  }

  InvocationResponse convert(ProtoInvocationResponse response) {
    try {
      return new InvocationResponse(
        convert(response.getMain()),
        convert(response.getOutput()),
        response.getInvId(),
        response.getFb(),
        response.getMacroIdsMap(),
        convert(response.getStatus()),
        convert(response.getStats()),
        response.getAsync(),
        objectMapper.readValue(response.getBody().toByteArray(), ObjectNode.class)
      );
    } catch (IOException e) {
      throw new InvocationException("", e);
    }
  }

  InvocationStatus convert(ProtoTaskStatus taskStatus) {
    return switch (taskStatus) {
      case LAZY -> InvocationStatus.LAZY;
      case DOING -> InvocationStatus.DOING;
      case SUCCEEDED -> InvocationStatus.SUCCEEDED;
      case FAILED, UNRECOGNIZED -> InvocationStatus.FAILED;
      case DEPENDENCY_FAILED -> InvocationStatus.DEPENDENCY_FAILED;
      case READY -> InvocationStatus.READY;
    };
  }

  InvocationStats convert(ProtoInvocationStats stats) {
    return new InvocationStats(stats.getQueTs(), stats.getSmtTs(), stats.getCptTs());
  }

  OObject convert(ProtoOObject object) {
    try {
      return new OObject(
        object.getKey(),
        object.getRevision(),
        object.getCls(),
        convert(object.getState()),
        DSMap.copy(object.getRefsMap()),
        objectMapper.readValue(object.getData().toByteArray(), ObjectNode.class),
        object.getLastOffset(),
        object.getLastInv()
      );
    } catch (IOException e) {
      throw new InvocationException("", e);
    }
  }

  OaasObjectState convert(OOState state) {
    return new OaasObjectState(
      DSMap.copy(state.getOverrideUrlsMap()),
      DSMap.copy(state.getVerIdsMap())
    );
  }

}
