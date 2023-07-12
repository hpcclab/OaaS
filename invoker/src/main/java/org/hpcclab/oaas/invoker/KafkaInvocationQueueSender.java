package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import org.hpcclab.oaas.invocation.InvocationQueueSender;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class KafkaInvocationQueueSender implements InvocationQueueSender {
  private static final Logger logger = LoggerFactory.getLogger( KafkaInvocationQueueSender.class );
  @Inject
  KafkaProducer<String, Buffer> producer;
  @Inject
  InvokerConfig config;
  @Override
  public Uni<Void> send(InvocationRequest request) {
    var topic = selectTopic(request);
    var kafkaRecord = KafkaProducerRecord.create(
        topic,
        request.partKey(),
        Json.encodeToBuffer(request)
      );
    if (logger.isDebugEnabled())
      logger.debug("send {} [{} {} {}]", topic, request.invId(), request.partKey(), request.outId());
    if (request.invId() != null)
      kafkaRecord.addHeader("ce_id", request.invId());
    return producer.send(kafkaRecord)
      .replaceWithVoid();
  }

  public String selectTopic(InvocationRequest request) {
    return config.invokeTopicPrefix() + request.cls();
  }
}
