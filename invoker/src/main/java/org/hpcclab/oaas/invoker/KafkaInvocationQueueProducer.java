package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invoker.ispn.lookup.HashUtil;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class KafkaInvocationQueueProducer implements InvocationQueueProducer {
  private static final Logger logger = LoggerFactory.getLogger(KafkaInvocationQueueProducer.class);
  @Inject
  KafkaProducer<String, Buffer> producer;
  @Inject
  InvokerConfig config;
  @Inject
  ClassRepository clsRepo;

  @Override
  public Uni<Void> offer(InvocationRequest request) {
    var topic = selectTopic(request);
    var conf =
      clsRepo.get(request.cls()).getConfig();
    var partition = HashUtil.getHashed(
      request.partKey(),
      conf==null ? OClassConfig.DEFAULT_PARTITIONS: conf.getPartitions()
    );
    KafkaProducerRecord<String, Buffer> kafkaRecord = KafkaProducerRecord.create(
      topic,
      request.partKey(),
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
