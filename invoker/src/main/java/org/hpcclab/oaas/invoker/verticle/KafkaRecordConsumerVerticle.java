package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecords;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.dispatcher.InvocationReqHolder;
import org.hpcclab.oaas.invoker.dispatcher.RecordDispatcher;
import org.hpcclab.oaas.invoker.ispn.SegmentCoordinator;
import org.hpcclab.oaas.invoker.mq.OffsetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaRecordConsumerVerticle extends AbstractVerticle {
  public static final long RETRY_DELAY = 1000;
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRecordConsumerVerticle.class);
  public final Duration timeout = Duration.ofMillis(500);
  private final KafkaConsumer<String, Buffer> consumer;
  private final RecordDispatcher recordDispatcher;
  private final OffsetManager offsetManager;
  private final SegmentCoordinator segmentCoordinator;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean isPolling = new AtomicBoolean(false);

  public KafkaRecordConsumerVerticle(SegmentCoordinator segmentCoordinator,
                                     KafkaConsumer<String, Buffer> consumer,
                                     RecordDispatcher recordDispatcher,
                                     InvokerConfig config) {
    this.consumer = consumer;
    this.recordDispatcher = recordDispatcher;
    this.offsetManager = recordDispatcher.getOffsetManager();
    this.segmentCoordinator = segmentCoordinator;
  }

  @Override
  public Uni<Void> asyncStart() {
    LOGGER.info("[{}] starting task consumer verticle", segmentCoordinator.getCls().getKey());
    consumer.exceptionHandler(this::handleException);
    consumer.partitionsRevokedHandler(offsetManager::handlePartitionRevoked);
    recordDispatcher.setDrainHandler(this::poll);
    offsetManager.setPeriodicCommit(vertx);
    return vertx.executeBlocking(() -> {
        segmentCoordinator.init(this::poll);
        return 0;
      })
      .call(segmentCoordinator::updateParts)
      .replaceWithVoid();
  }

  @Override
  public Uni<Void> asyncStop() {
    LOGGER.info("[{}] stopping task consumer verticle", segmentCoordinator.getCls().getKey());
    closed.set(true);
    offsetManager.removePeriodicCommit(vertx);
    segmentCoordinator.close();
    return recordDispatcher.waitTillQueueEmpty()
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

  private void handleRecords(KafkaConsumerRecords<String, Buffer> records) {
    if (LOGGER.isDebugEnabled() && !records.isEmpty())
      LOGGER.debug("[{}] receiving {} records", segmentCoordinator.getCls().getKey(), records.size());
    List<InvocationReqHolder> reqHolders = new ArrayList<>(records.size());
    for (int i = 0; i < records.size(); i++) {
      reqHolders.add(
        InvocationReqHolder.from(records.recordAt(i))
      );
    }
    recordDispatcher.dispatch(reqHolders);
    isPolling.set(false);
    if (recordDispatcher.canConsume()) {
      poll();
    }
  }

  private void handleException(Throwable throwable) {
    LOGGER.error("catch error", throwable);
    vertx.setTimer(RETRY_DELAY, l -> poll());
  }

  private void handlePollException(Throwable throwable) {
    LOGGER.error("catch error when poll", throwable);
    isPolling.set(false);
    vertx.setTimer(RETRY_DELAY, l -> poll());
  }
}
