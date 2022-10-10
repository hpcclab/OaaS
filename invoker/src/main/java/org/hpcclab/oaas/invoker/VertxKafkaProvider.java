package org.hpcclab.oaas.invoker;

import io.vertx.kafka.client.common.KafkaClientOptions;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.util.HashMap;
import java.util.Map;

@Default
public class VertxKafkaProvider {

  @Produces
  public KafkaClientOptions options(InvokerConfig invokerConfig) {

    Map<String, Object> config = new HashMap<>();
    config.put("bootstrap.servers", invokerConfig.kafka());
    config.put("key.deserializer", "io.vertx.kafka.client.serialization.BufferDeserializer");
    config.put("value.deserializer", "io.vertx.kafka.client.serialization.BufferDeserializer");
    config.put("group.id", invokerConfig.kafkaGroup());
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "true");
    return new KafkaClientOptions()
      .setConfig(config);
  }
}
