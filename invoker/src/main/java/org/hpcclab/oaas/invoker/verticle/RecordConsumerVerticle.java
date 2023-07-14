package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.OffsetManager;
import org.hpcclab.oaas.invoker.TaskVerticlePoolDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordConsumerVerticle extends AbstractVerticle {
  public static final long RETRY_DELAY = 200;
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordConsumerVerticle.class);
  public final Duration timeout = Duration.ofMillis(500);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean isPolling = new AtomicBoolean(false);
  private final int numberOfVerticle;
  KafkaConsumer<String, Buffer> consumer;
  TaskVerticlePoolDispatcher taskDispatcher;
  OffsetManager offsetManager;
  Set<String> topics = Set.of();

  public RecordConsumerVerticle(KafkaConsumer<String, Buffer> consumer,
                                TaskVerticlePoolDispatcher taskDispatcher,
                                InvokerConfig config) {
    this.consumer = consumer;
    this.taskDispatcher = taskDispatcher;
    this.offsetManager = taskDispatcher.getOffsetManager();
    numberOfVerticle = config.numOfInvokerVerticle();
  }

  @Override
  public Uni<Void> asyncStart() {
    LOGGER.info("starting task consumer verticle for topics [{}]", topics);
    consumer.exceptionHandler(this::handleException);
    consumer.partitionsRevokedHandler(offsetManager::handlePartitionRevoked);
    taskDispatcher.setDrainHandler(this::poll);
    offsetManager.setPeriodicCommit(vertx);
    return taskDispatcher.deploy(numberOfVerticle)
      .call(() -> consumer.subscribe(topics))
      .invoke(this::poll);
  }

  @Override
  public Uni<Void> asyncStop() {
    LOGGER.info("stopping task consumer verticle for topics {}", topics);
    closed.set(true);
    offsetManager.removePeriodicCommit(vertx);
    return taskDispatcher.waitTillQueueEmpty()
      .call(offsetManager::commitAll);
  }

  public Set<String> getTopics() {
    return topics;
  }

  public void setTopics(Set<String> topics) {
    this.topics = topics;
  }

  public void poll() {
    if (closed.get() || isPolling.get())
      return;
    if (isPolling.compareAndSet(false, true)) {
      consumer.poll(timeout)
        .subscribe()
        .with(this::handleRecords, this::handlePollException);
    }
  }

  private void handleRecords(KafkaConsumerRecords<String, Buffer> records) {
    if (LOGGER.isDebugEnabled() && records.size() > 0)
      LOGGER.debug("{} receiving {} records", topics, records.size());
    taskDispatcher.offer(records);
    isPolling.set(false);
    if (taskDispatcher.canConsume()) {
      poll();
    }
  }

  private void handleException(Throwable throwable) {
    LOGGER.error("catch error", throwable);
  }

  private void handlePollException(Throwable throwable) {
    LOGGER.error("catch error", throwable);
    vertx.setTimer(RETRY_DELAY, l -> poll());
  }
}
