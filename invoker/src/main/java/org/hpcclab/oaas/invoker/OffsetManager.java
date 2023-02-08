package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OffsetManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(OffsetManager.class);
  static final long COMMIT_INTERVAL = 10000L;
  final Map<TopicPartition, OffsetTracker> trackerMap;
  KafkaConsumer<?, ?> kafkaConsumer;

  long timerId = -1;

  public OffsetManager(KafkaConsumer<?, ?> kafkaConsumer) {
    this.kafkaConsumer = kafkaConsumer;
    this.trackerMap = new ConcurrentHashMap<>();
  }

  public void setPeriodicCommit(Vertx vertx) {
    timerId = vertx.setPeriodic(COMMIT_INTERVAL,
      l -> commitAll().subscribe().asCompletionStage());
  }

  public void removePeriodicCommit(Vertx vertx) {
    vertx.cancelTimer(timerId);
  }

  public void handlePartitionRevoked(Set<TopicPartition> topicPartitions) {
    commit(topicPartitions)
      .subscribe()
      .with(__ -> {
          for (TopicPartition topicPartition : topicPartitions) {
            trackerMap.remove(topicPartition);
          }
        },
        err -> LOGGER.error("catch error while committing", err));
  }

  public void recordDone(KafkaConsumerRecord<?, ?> rec) {
    var partition = new TopicPartition(rec.topic(), rec.partition());
    var tracker = trackerMap.get(partition);
    if (tracker==null)
      return;
    tracker.recordDone(rec.offset());
  }

  public void recordReceived(KafkaConsumerRecord<?, ?> rec) {
    var partition = new TopicPartition(rec.topic(), rec.partition());
    trackerMap.computeIfAbsent(partition,
      k -> new OffsetTracker(rec.offset()));
  }

  public Uni<Void> commit(Set<TopicPartition> partitions) {
    var map = partitions.stream()
      .map(part -> {
        var tracker = trackerMap.get(part);
        if (tracker==null) return null;
        return Map.entry(part, new OffsetAndMetadata(tracker.offsetToCommit(), ""));
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("committing {}", map);
    }
    Uni<Map<TopicPartition, OffsetAndMetadata>> uni = UniHelper
      .toUni(kafkaConsumer.getDelegate().commit(map));
    return uni
      .replaceWithVoid();
  }


  public Uni<Void> commitAll() {
    var map = trackerMap.entrySet()
      .stream()
      .map(entry -> Map.entry(entry.getKey(),
        new OffsetAndMetadata(entry.getValue().offsetToCommit(), "")))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("committing {}", map);
    }
    Future<Map<TopicPartition, OffsetAndMetadata>> future =
      kafkaConsumer.getDelegate().commit(map);
    return UniHelper.toUni(future)
      .onFailure().invoke(e -> LOGGER.error("fail to commit {}", map, e))
      .replaceWithVoid();
  }
}
