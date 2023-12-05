package org.hpcclab.oaas.provisioner.handler;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.*;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.provisioner.provisioner.KafkaProvisioner;
import org.hpcclab.oaas.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ClassMsgHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassMsgHandler.class);

  @Inject
  KafkaProvisioner kafkaProvisioner;
  @Inject
  ClassRepository classRepository;
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
    if (cls.isMarkForRemoval())
      return;

    var updater = kafkaProvisioner.provision(cls);
    if (updater!=null) {
      var updatedCls = classRepository.compute(cls.getKey(), (k, v) -> {
        updater.accept(v);
        return v;
      });
        var msg = Message.of(updatedCls, Metadata.of(
          OutgoingKafkaRecordMetadata.builder()
            .withKey(updatedCls.getKey())
            .withHeaders(new RecordHeaders()
              .add("oprc-provision-skip", "true".getBytes(StandardCharsets.UTF_8))
            )
            .build()
        ));
        emitter.send(msg);
    }
  }
}
