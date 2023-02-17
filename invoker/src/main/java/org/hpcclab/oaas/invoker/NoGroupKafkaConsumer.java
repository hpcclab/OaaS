package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class NoGroupKafkaConsumer<T> {
  protected KafkaConsumer<String, Buffer> client;
  protected String topic;

  public NoGroupKafkaConsumer() {
  }

  protected NoGroupKafkaConsumer(Vertx vertx,
                                 KafkaClientOptions options,
                                 String topic) {
    this.topic = topic;

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

  void clean() {
    client.closeAndAwait();
  }

  abstract T deserialize(Buffer buffer);

  public Uni<Void> start() {
    return client
      .partitionsFor(topic)
      .flatMap(partitionInfos -> {
        var parts = partitionInfos
          .stream()
          .map(partitionInfo -> new TopicPartition(partitionInfo.getTopic(), partitionInfo.getPartition()))
          .collect(Collectors.toSet());
        return client.assign(parts)
          .call(() -> client.seekToEnd(parts));
      });
  }

  public void setHandler(Consumer<T> consumer) {
    client.handler(kafkaRecord -> consumer.accept(deserialize(kafkaRecord.value())));
  }
}
