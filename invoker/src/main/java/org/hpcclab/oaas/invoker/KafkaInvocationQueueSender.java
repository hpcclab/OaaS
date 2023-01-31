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

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

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
    var key = request.immutable()? null: request.partKey();
    var kafkaRecord = KafkaProducerRecord.create(
        topic,
        key,
        Json.encodeToBuffer(request)
      )
      .addHeader("ce_function", request.function());
    if (logger.isDebugEnabled())
      logger.debug("send {} [{} {} {}]", topic, request.invId(), request.partKey(), request.outId());
    if (request.invId() != null)
      kafkaRecord.addHeader("ce_id", request.invId());
    return producer.send(kafkaRecord)
      .replaceWithVoid();
  }

  public String selectTopic(InvocationRequest request) {
    return config.functionTopicPrefix() + request.function();
  }
}
