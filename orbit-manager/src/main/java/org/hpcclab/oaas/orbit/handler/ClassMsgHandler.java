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
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.orbit.provisioner.KafkaProvisioner;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;
import org.hpcclab.oaas.proto.OClassStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ClassMsgHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassMsgHandler.class);

  @Inject
  KafkaProvisioner kafkaProvisioner;
  @GrpcClient("class-manager")
  DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater;
  @Inject
  ProtoMapper mapper;
  @Channel("clsUpdated")
  Emitter<OClass> emitter;

  @Incoming("clsProvisions")
  @RunOnVirtualThread
  public void handle(ConsumerRecord<String, OClass> clsRecord) {
    var header = clsRecord.headers().lastHeader("oprc-provision-skip");
    if (header!=null && new String(header.value()).equals("true")) {
      return;
    }
    LOGGER.debug("Received class provision: {}", clsRecord.key());
    var cls = clsRecord.value();
    if (cls.isMarkForRemoval()) return;

    var updatedCls = kafkaProvisioner.provision(cls);
    if (updatedCls!=null) {
      var statusUpdate = OClassStatusUpdate.newBuilder().setKey(cls.getKey()).setStatus(mapper.toProto(updatedCls.getStatus())).build();
      var updatedProto = deploymentStatusUpdater.updateCls(statusUpdate);
      updatedCls = mapper.fromProto(updatedProto);
      var msg = Message.of(updatedCls, Metadata.of(OutgoingKafkaRecordMetadata.builder().withKey(updatedCls.getKey()).withHeaders(new RecordHeaders().add("oprc-provision-skip", "true".getBytes(StandardCharsets.UTF_8))).build()));
      emitter.send(msg);
    }
  }
}
