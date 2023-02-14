package org.hpcclab.oaas.invoker.producer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.serialization.BufferDeserializer;
import io.vertx.kafka.client.serialization.BufferSerializer;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.hpcclab.oaas.invoker.InvokerConfig;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import java.util.HashMap;
import java.util.Map;

@Dependent
@RegisterForReflection(
  targets = {
    BufferSerializer.class,
    BufferDeserializer.class,
  }
)
public class VertxKafkaClientProducer {

  @Produces
  public KafkaClientOptions options(InvokerConfig invokerConfig) {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, invokerConfig.kafka());
    config.put(ConsumerConfig.GROUP_ID_CONFIG, invokerConfig.kafkaGroup());
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    return new KafkaClientOptions()
      .setConfig(config);
  }

  @Produces
  public KafkaProducer<String, Buffer> producer(Vertx vertx,
                                                InvokerConfig invokerConfig) {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, invokerConfig.kafka());
    var options = new KafkaClientOptions()
      .setConfig(config);

    return KafkaProducer.createShared(
      vertx, "default", options, String.class, Buffer.class
    );
  }
}
