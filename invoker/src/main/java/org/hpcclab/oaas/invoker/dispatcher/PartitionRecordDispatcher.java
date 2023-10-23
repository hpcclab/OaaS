package org.hpcclab.oaas.invoker.dispatcher;

import io.netty.util.internal.ThreadLocalRandom;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.OffsetManager;
import org.hpcclab.oaas.invoker.verticle.RecordHandlerVerticle;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class PartitionRecordDispatcher<R extends KafkaConsumerRecord<?, ?>> implements  RecordDispatcher{
  private static final Logger logger = LoggerFactory.getLogger(PartitionRecordDispatcher.class);
  private final AtomicInteger inflight = new AtomicInteger(0);
  private final OffsetManager offsetManager;
  private final int maxInflight;
  String name = "unknown";
  Random random = ThreadLocalRandom.current();
  private ImmutableList<PartitionRecordHandler<R>> partitions = Lists.immutable.empty();
  private Runnable drainHandler;

  public PartitionRecordDispatcher(OffsetManager offsetManager,
                                   InvokerConfig config) {
    this.offsetManager = offsetManager;
    this.maxInflight = config.maxInflight();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDrainHandler(Runnable drainHandler) {
    this.drainHandler = drainHandler;
  }

  public boolean canConsume() {
    return inflight.get() < maxInflight;
  }


  public void setPartitions(ImmutableList<PartitionRecordHandler<R>> partitions) {
    this.partitions = partitions;
    for (PartitionRecordHandler<R> partition : partitions) {
      partition.setOnRecordCompleteHandler(this::handleRecordComplete);
    }
  }

  private void handleRecordComplete(R rec) {
    offsetManager.recordDone(rec);
    if (drainHandler!=null && inflight.decrementAndGet() < maxInflight)
      drainHandler.run();
  }

  public void dispatch(KafkaConsumerRecords<?, ?> records) {
    if (partitions.isEmpty())
      throw new IllegalStateException("Must deploy first");
    for (int i = 0; i < records.size(); i++) {
      var rec = records.recordAt(i);
      offsetManager.recordReceived(rec);
      var partition = partitions.getFirst();
      var size = partitions.size();
      int hashIndex = 0;
      if (size!=1) {
        if (rec.key()!=null) {
          hashIndex = rec.key().hashCode() % size;
          if (hashIndex < 0) hashIndex = -hashIndex;
        } else {
          hashIndex  = random.nextInt(size);
        }
        partition = partitions.get(hashIndex);
      }
      if (logger.isDebugEnabled())
        logger.debug("dispatch {} {} {}", hashIndex, rec.key(), rec.offset());
      partition.offer((R) rec);
    }
  }

  public Uni<Void> waitTillQueueEmpty() {
    return Multi.createFrom().ticks().every(Duration.ofMillis(100))
        .filter(l -> partitions.stream()
        .mapToInt(PartitionRecordHandler::countPending)
        .sum()==0)
      .toUni()
      .replaceWithVoid();
  }

  public OffsetManager getOffsetManager() {
    return offsetManager;
  }
}
