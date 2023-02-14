package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
public class FunctionListener {
  @Inject
  Vertx vertx;
  @Inject
  KafkaClientOptions options;
  KafkaConsumer<String, Buffer> client;
  @Inject
  InvokerConfig config;

  @PostConstruct
  void setup() {
    var kafkaConfig = options.getConfig();
    var modOption = new KafkaClientOptions();
    var newConfig = new HashMap<String, Object>();
    newConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig
      .get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
    newConfig.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1");

    client = KafkaConsumer.create(
      vertx, modOption.setConfig(newConfig), String.class, Buffer.class
    );

  }

  @PreDestroy
  void clean() {
    client.closeAndAwait();
  }

  public Uni<Void> start() {
    return client
      .partitionsFor(config.fnProvisionTopic())
      .flatMap(partitionInfos -> {
        var parts = partitionInfos
          .stream()
          .map(partitionInfo -> new TopicPartition(partitionInfo.getTopic(), partitionInfo.getPartition()))
          .collect(Collectors.toSet());
        return client.assign(parts)
          .call(() -> client.seekToEnd(parts));
      });
  }

  public void setHandler(Consumer<OaasFunction> consumer) {
    client.handler(kafkaRecord -> {
      var fn = Json.decodeValue(kafkaRecord.value(), OaasFunction.class);
      consumer.accept(fn);
    });
  }
}
