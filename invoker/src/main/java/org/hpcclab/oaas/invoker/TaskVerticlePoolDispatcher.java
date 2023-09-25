package org.hpcclab.oaas.invoker;

import io.netty.util.internal.ThreadLocalRandom;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.hpcclab.oaas.invoker.verticle.RecordHandlerVerticle;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskVerticlePoolDispatcher<R extends KafkaConsumerRecord<?, ?>> {
  private static final Logger logger = LoggerFactory.getLogger(TaskVerticlePoolDispatcher.class);
  private final AtomicInteger inflight = new AtomicInteger(0);
  private final Context context;
  private final OffsetManager offsetManager;
  private final int maxInflight;
  private final Vertx vertx;
  private final VerticleFactory<? extends RecordHandlerVerticle<R>> invokerVerticleFactory;
  String name = "unknown";
  Random random = ThreadLocalRandom.current();
  private ImmutableList<RecordHandlerVerticle<R>> verticles = Lists.immutable.empty();
  private Runnable drainHandler;

  public TaskVerticlePoolDispatcher(Vertx vertx,
                                    VerticleFactory<? extends RecordHandlerVerticle<R>> invokerVerticleFactory,
                                    OffsetManager offsetManager,
                                    InvokerConfig config) {
    this.vertx = vertx;
    this.invokerVerticleFactory = invokerVerticleFactory;
    this.context = vertx.getOrCreateContext();
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

  public Uni<Void> deploy(int count) {
    return Multi.createFrom().range(0, count)
      .map(this::buildVerticle)
      .onItem()
      .call(vertx::deployVerticle)
      .collect()
      .asList()
      .invoke(list -> verticles = Lists.immutable.ofAll(list))
      .replaceWithVoid();
  }

  private RecordHandlerVerticle<R> buildVerticle(int i) {
    var verticle = invokerVerticleFactory.createVerticle();
    verticle.setName("invoker-verticle-" + name + "-" + i);
    verticle.setOnRecordCompleteHandler(this::handleRecordComplete);
    return verticle;
  }

  private void handleRecordComplete(R rec) {
    context.runOnContext(() -> {
      offsetManager.recordDone(rec);
      if (drainHandler!=null && inflight.decrementAndGet() < maxInflight)
        drainHandler.run();
    });
  }

  public void dispatch(KafkaConsumerRecords<?, ?> records) {
    if (verticles.isEmpty())
      throw new IllegalStateException("Must deploy first");
    for (int i = 0; i < records.size(); i++) {
      var rec = records.recordAt(i);
      offsetManager.recordReceived(rec);
      var verticle = verticles.getFirst();
      var size = verticles.size();
      if (size!=1) {
        if (rec.key()!=null) {
          var hashIndex = rec.key().hashCode() % size;
          if (hashIndex < 0) hashIndex = -hashIndex;
          verticle = verticles.get(hashIndex);
        } else {
          verticle = verticles.get(random.nextInt(size));
        }
      }
      if (logger.isDebugEnabled())
        logger.debug("dispatch {} {} {}", rec.key(), rec.offset(), verticle.getName());
      verticle.offer((R) rec);
    }
  }

  public Uni<Void> waitTillQueueEmpty() {
    return Multi.createFrom().ticks().every(Duration.ofMillis(100))
      .filter(l -> verticles.stream()
        .mapToInt(RecordHandlerVerticle::countQueueingTasks)
        .sum()==0)
      .toUni()
      .replaceWithVoid();
  }

  public OffsetManager getOffsetManager() {
    return offsetManager;
  }
}
