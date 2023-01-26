package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.hpcclab.oaas.invoker.verticle.AbstractOrderedRecordVerticle;
import org.hpcclab.oaas.invoker.verticle.OrderedTaskInvokerVerticle;
import org.hpcclab.oaas.invoker.verticle.RecordHandlerVerticle;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskVerticlePoolDispatcher {
  private final AtomicInteger inflight = new AtomicInteger(0);
  private final Context context;
  private final OffsetManager offsetManager;
  private final int maxInflight;
  private final Vertx vertx;
  private final VerticleFactory<? extends AbstractOrderedRecordVerticle> invokerVerticleFactory;
  private List<? extends RecordHandlerVerticle<KafkaConsumerRecord<String, Buffer>>> verticles = List.of();
  private Runnable drainHandler;
  String name = "unknown";
  Random random = new Random();

  public TaskVerticlePoolDispatcher(Vertx vertx,
                                    VerticleFactory<? extends AbstractOrderedRecordVerticle> invokerVerticleFactory,
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
      .invoke(list -> verticles = list)
      .replaceWithVoid();
  }

  private AbstractOrderedRecordVerticle buildVerticle(int i) {
    var verticle = invokerVerticleFactory.createVerticle();
    verticle.setName("invoker-verticle-" +name+ "-" + i);
    verticle.setOnRecordCompleteHandler(this::handleRecordComplete);
    return verticle;
  }

  private void handleRecordComplete(KafkaConsumerRecord<String, Buffer> rec) {
    context.runOnContext(() -> {
      offsetManager.recordDone(rec);
      if (drainHandler!=null && inflight.decrementAndGet() < maxInflight)
        drainHandler.run();
    });
  }

  public void offer(KafkaConsumerRecords<String, Buffer> records) {
    if (verticles.isEmpty())
      throw new IllegalStateException("Must deploy first");
    for (int i = 0; i < records.size(); i++) {
      var rec = records.recordAt(i);
      offsetManager.recordReceived(rec);
      if (verticles.size()==1) {
        verticles.get(0).offer(rec);
      } else if (rec.key() !=null) {
        var hashIndex = rec.key().hashCode() % verticles.size();
        var verticle = verticles.get(hashIndex);
        verticle.offer(rec);
      } else {
        var verticle =verticles.get(random.nextInt(verticles.size()));
        verticle.offer(rec);
      }
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
