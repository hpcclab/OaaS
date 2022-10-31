package org.hpcclab.oaas.invoker.producer;

import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import org.apache.kafka.clients.KafkaClient;
import org.hpcclab.oaas.invoker.InvokerConfig;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Dependent
public class VertxKafkaClientProducer {

  @Produces
  public KafkaClientOptions options(InvokerConfig invokerConfig) {
    Map<String, Object> config = new HashMap<>();
    config.put("bootstrap.servers", invokerConfig.kafka());
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "io.vertx.kafka.client.serialization.BufferDeserializer");
    config.put("group.id", invokerConfig.kafkaGroup());
    config.put("auto.offset.reset", "earliest");
    config.put("fetch.min.bytes", "1");
    config.put("enable.auto.commit", "true");
    return new KafkaClientOptions()
      .setConfig(config);
  }

  @Produces
  public KafkaProducer<String, Buffer> producer(Vertx vertx,
                                                InvokerConfig invokerConfig) {
    Map<String, Object> config = new HashMap<>();
    config.put("bootstrap.servers", invokerConfig.kafka());
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "io.vertx.kafka.client.serialization.BufferSerializer");
    config.put("enable.auto.commit", "true");
    var options = new KafkaClientOptions()
      .setConfig(config);

    return KafkaProducer.createShared(vertx, "default",options);
  }
}
