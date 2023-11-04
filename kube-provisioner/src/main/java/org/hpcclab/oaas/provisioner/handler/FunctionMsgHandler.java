package org.hpcclab.oaas.provisioner.handler;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.*;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.provisioner.provisioner.KnativeProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class FunctionMsgHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionMsgHandler.class);
  @Inject
  ArgFunctionRepository functionRepo;
  @Inject
  KnativeProvisioner knativeProvisioner;
  @Channel("fnUpdated")
  Emitter<OaasFunction> emitter;


  @Incoming("fnProvisions")
  @RunOnVirtualThread
  public void handle(ConsumerRecord<String, OaasFunction> functionRecord) {
    var header = functionRecord.headers().lastHeader("oprc-provision-skip");
    if (header!=null && new String(header.value()).equals("true")) {
      return;
    }
    LOGGER.debug("Received function provision: {}", functionRecord.key());
    var function = functionRecord.value();

    if (function!=null &&
      function.getProvision()!=null &&
      function.getProvision().getKnative()!=null) {
      var functionUpdater = knativeProvisioner.provision(function);
      var updatedFunc = functionRepo.compute(function.getKey(), (k, f) -> {
        functionUpdater.accept(f);
        return f;
      });
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
