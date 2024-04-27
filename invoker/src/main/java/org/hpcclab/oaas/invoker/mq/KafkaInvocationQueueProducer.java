package org.hpcclab.oaas.invoker.mq;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import jakarta.enterprise.context.Dependent;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.lookup.HashUtil;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class KafkaInvocationQueueProducer implements InvocationQueueProducer {
  private static final Logger logger = LoggerFactory.getLogger(KafkaInvocationQueueProducer.class);

  final KafkaProducer<String, Buffer> producer;
  final InvokerConfig config;
  final ClassControllerRegistry registry;

  public KafkaInvocationQueueProducer(KafkaProducer<String, Buffer> producer, InvokerConfig config, ClassControllerRegistry registry) {
    this.producer = producer;
    this.config = config;
    this.registry = registry;
  }

  @Override
  public Uni<Void> offer(InvocationRequest request) {
    var topic = selectTopic(request);
    var conf = registry.getClassController(request.cls())
      .getCls()
      .getConfig();
    var partKey = request.partKey();
    Integer partition = null;
    if (partKey!=null) {
      partition = HashUtil.getHashed(
        partKey,
        conf==null ? OClassConfig.DEFAULT_PARTITIONS:conf.getPartitions()
      );
    }
    KafkaProducerRecord<String, Buffer> kafkaRecord = KafkaProducerRecord.create(
      topic,
      partKey,
      serialize(request),
      null,
      partition
    );
    if (logger.isDebugEnabled())
      logger.debug("send {} [{} {} {}, {}]", topic, request.invId(), request.partKey(), request.outId(), partition);
    if (request.invId()!=null)
      kafkaRecord.addHeader("ce_id", request.invId());
    return producer.send(kafkaRecord)
      .replaceWithVoid();
  }

  public Buffer serialize(InvocationRequest request) {
    return Json.encodeToBuffer(request);
  }

  public String selectTopic(InvocationRequest request) {
    return config.invokeTopicPrefix() + request.cls();
  }
}
