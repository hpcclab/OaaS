package org.hpcclab.oaas.invoker.cdi;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.serialization.BufferDeserializer;
import io.vertx.kafka.client.serialization.BufferSerializer;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.hpcclab.oaas.invoker.InvokerConfig;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
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
