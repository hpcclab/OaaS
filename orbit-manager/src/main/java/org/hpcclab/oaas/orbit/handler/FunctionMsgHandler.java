package org.hpcclab.oaas.orbit.handler;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.*;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.orbit.provisioner.KnativeProvisioner;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class FunctionMsgHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionMsgHandler.class);
  @GrpcClient("class-manager")
  DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater;

  @Inject
  KnativeProvisioner knativeProvisioner;
  @Inject
  ProtoMapper mapper;
  @Channel("fnUpdated")
  Emitter<OFunction> emitter;


  @Incoming("fnProvisions")
  @RunOnVirtualThread
  public void handle(ConsumerRecord<String, OFunction> functionRecord) {
    var header = functionRecord.headers().lastHeader("oprc-provision-skip");
    if (header!=null && new String(header.value()).equals("true")) {
      return;
    }
    LOGGER.debug("Received function provision: {}", functionRecord.key());
    var function = functionRecord.value();

    if (function!=null &&
      function.getProvision()!=null &&
      function.getProvision().getKnative()!=null) {
      var updatedFunction = knativeProvisioner.provision(function);
      var statusUpdate = OFunctionStatusUpdate.newBuilder()
        .setKey(updatedFunction.getKey())
        .setStatus(mapper.toProto(updatedFunction.getStatus()))
        .build();
      var updatedFunc = mapper.fromProto(deploymentStatusUpdater
        .updateFn(statusUpdate));
      var msg = Message.of(updatedFunc, Metadata.of(
          OutgoingKafkaRecordMetadata.builder()
            .withKey(updatedFunc.getKey())
            .withHeaders(new RecordHeaders()
              .add("oprc-provision-skip", "true".getBytes(StandardCharsets.UTF_8))
            )
            .build()
        )
      );
      emitter.send(msg);
    }
  }
}
