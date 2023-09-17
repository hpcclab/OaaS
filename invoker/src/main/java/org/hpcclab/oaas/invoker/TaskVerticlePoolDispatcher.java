package org.hpcclab.oaas.invoker;

import io.netty.util.internal.ThreadLocalRandom;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.hpcclab.oaas.invoker.verticle.RecordHandlerVerticle;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class TaskVerticlePoolDispatcher {
  private static final Logger logger = LoggerFactory.getLogger( TaskVerticlePoolDispatcher.class );
  private final AtomicInteger inflight = new AtomicInteger(0);
  private final Context context;
  private final OffsetManager offsetManager;
  private final int maxInflight;
  private final Vertx vertx;
  private final VerticleFactory<? extends RecordHandlerVerticle<KafkaConsumerRecord>> invokerVerticleFactory;
  private RecordHandlerVerticle<KafkaConsumerRecord>[] verticles = new RecordHandlerVerticle[0];
  private Runnable drainHandler;
  String name = "unknown";
  Random random = ThreadLocalRandom.current();

  public TaskVerticlePoolDispatcher(Vertx vertx,
                                    VerticleFactory<? extends RecordHandlerVerticle<KafkaConsumerRecord>> invokerVerticleFactory,
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
      .invoke(list -> {
        verticles = list.toArray(new RecordHandlerVerticle[list.size()]);
      })
      .replaceWithVoid();
  }

  private RecordHandlerVerticle<KafkaConsumerRecord> buildVerticle(int i) {
    var verticle = invokerVerticleFactory.createVerticle();
    verticle.setName("invoker-verticle-" +name+ "-" + i);
    verticle.setOnRecordCompleteHandler(this::handleRecordComplete);
    return verticle;
  }

  private void handleRecordComplete(KafkaConsumerRecord<?, ?> rec) {
    context.runOnContext(() -> {
      offsetManager.recordDone(rec);
      if (drainHandler!=null && inflight.decrementAndGet() < maxInflight)
        drainHandler.run();
    });
  }

  public void dispatch(KafkaConsumerRecords<String, Buffer> records) {
    if (verticles.length == 0)
      throw new IllegalStateException("Must deploy first");
    for (int i = 0; i < records.size(); i++) {
      var rec = records.recordAt(i);
      offsetManager.recordReceived(rec);
      var verticle = verticles[0];
        if (verticles.length!=1) {
            if (rec.key() !=null) {
              var hashIndex = rec.key().hashCode() % verticles.length;
              if (hashIndex < 0) hashIndex = -hashIndex;
              verticle = verticles[hashIndex];
            } else {
              verticle =verticles[random.nextInt(verticles.length)];
            }
        }
      if (logger.isDebugEnabled())
        logger.debug("dispatch {} {} {}", rec.key(), rec.offset(), verticle.getName());
      verticle.offer(rec);
    }
  }

  public Uni<Void> waitTillQueueEmpty() {
    return Multi.createFrom().ticks().every(Duration.ofMillis(100))
      .filter(l -> Stream.of(verticles)
        .mapToInt(RecordHandlerVerticle::countQueueingTasks)
        .sum()==0)
      .toUni()
      .replaceWithVoid();
  }

  public OffsetManager getOffsetManager() {
    return offsetManager;
  }
}
