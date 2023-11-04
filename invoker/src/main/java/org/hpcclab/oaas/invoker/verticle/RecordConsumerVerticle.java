package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.OffsetManager;
import org.hpcclab.oaas.invoker.dispatcher.VerticlePoolRecordDispatcher;
import org.hpcclab.oaas.invoker.ispn.SegmentCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordConsumerVerticle<K, V> extends AbstractVerticle {
  public static final long RETRY_DELAY = 200;
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordConsumerVerticle.class);
  public final Duration timeout = Duration.ofMillis(500);
  final KafkaConsumer<K, V> consumer;
  final VerticlePoolRecordDispatcher<KafkaConsumerRecord<K, V>> taskDispatcher;
  final OffsetManager offsetManager;
  final SegmentCoordinator segmentCoordinator;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean isPolling = new AtomicBoolean(false);
  private final int numberOfVerticle;

  public RecordConsumerVerticle(SegmentCoordinator segmentCoordinator,
                                KafkaConsumer<K, V> consumer,
                                VerticlePoolRecordDispatcher<KafkaConsumerRecord<K, V>> taskDispatcher,
                                InvokerConfig config) {
    this.consumer = consumer;
    this.taskDispatcher = taskDispatcher;
    this.offsetManager = taskDispatcher.getOffsetManager();
    numberOfVerticle = config.numOfInvokerVerticle();
    this.segmentCoordinator = segmentCoordinator;
  }

  @Override
  public Uni<Void> asyncStart() {
    LOGGER.info("[{}] starting task consumer verticle", segmentCoordinator.getCls());
    consumer.exceptionHandler(this::handleException);
    consumer.partitionsRevokedHandler(offsetManager::handlePartitionRevoked);
    taskDispatcher.setDrainHandler(this::poll);
    offsetManager.setPeriodicCommit(vertx);
    return vertx.executeBlocking(() -> {
        segmentCoordinator.init();
        return 0;
      })
      .call(segmentCoordinator::updateParts)
      .call(__ ->
        taskDispatcher.deploy(numberOfVerticle))
      .invoke(this::poll)
      .replaceWithVoid();
  }

  @Override
  public Uni<Void> asyncStop() {
    LOGGER.info("[{}] stopping task consumer verticle", segmentCoordinator.getCls().getKey());
    closed.set(true);
    offsetManager.removePeriodicCommit(vertx);
    return taskDispatcher.waitTillQueueEmpty()
      .call(offsetManager::commitAll);
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

  private void handleRecords(KafkaConsumerRecords<K, V> records) {
    if (LOGGER.isDebugEnabled() && !records.isEmpty())
      LOGGER.debug("[{}] receiving {} records", segmentCoordinator.getCls().getKey(), records.size());
    taskDispatcher.dispatch(records);
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
