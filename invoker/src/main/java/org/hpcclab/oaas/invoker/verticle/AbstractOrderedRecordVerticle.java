package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class AbstractOrderedRecordVerticle<T> extends AbstractVerticle
  implements RecordHandlerVerticle<KafkaConsumerRecord<String, Buffer>> {
  private static final Logger logger = LoggerFactory.getLogger(AbstractOrderedRecordVerticle.class);

  protected Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler;
  protected String name = "unknown";
  final AtomicInteger inflight = new AtomicInteger(0);
  final AtomicBoolean lock = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<KafkaConsumerRecord<String, Buffer>> taskQueue;
  private final MutableListMultimap<String, KafkaConsumerRecord<String, Buffer>> pausedTask;
  private final ConcurrentHashSet<String> lockingTaskKeys = new ConcurrentHashSet<>();
  private final int maxConcurrent;

  public AbstractOrderedRecordVerticle(int maxConcurrent) {
    this.maxConcurrent = maxConcurrent;
    this.taskQueue = new ConcurrentLinkedQueue<>();
    this.pausedTask = Multimaps.mutable.list.empty();
  }

  @Override
  public void setOnRecordCompleteHandler(Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler) {
    this.onRecordCompleteHandler = onRecordCompleteHandler;
  }

  @Override
  public void offer(KafkaConsumerRecord<String, Buffer> taskRecord) {
    taskQueue.offer(taskRecord);
    if (lock.get())
      return;
    context.runOnContext(__ -> consume());
  }

  protected abstract boolean shouldLock(KafkaConsumerRecord<String, Buffer> taskRecord,
                                                  T parsedContent);

  private void consume() {
    logger.debug("{}: consuming[lock={}, inflight={}]", name, lock, inflight);
    if (!lock.compareAndSet(false, true)) {
      return;
    }
    while (inflight.get() < maxConcurrent) {
      var taskRecord = taskQueue.poll();
      if (taskRecord==null) {
        break;
      }
      if (taskRecord.key()==null) {
        inflight.incrementAndGet();
        var content = parseContent(taskRecord);
        handleRecord(taskRecord, content);
      } else if (lockingTaskKeys.contains(taskRecord.key())) {
        pausedTask.put(taskRecord.key(), taskRecord);
      } else {
        inflight.incrementAndGet();
        var content = parseContent(taskRecord);
        if (shouldLock(taskRecord, content)) {
          lockingTaskKeys.add(taskRecord.key());
        }
        handleRecord(taskRecord, content);
      }
    }

    lock.set(false);
  }

  protected abstract T parseContent(KafkaConsumerRecord<String, Buffer> taskRecord);

  protected abstract void handleRecord(KafkaConsumerRecord<String, Buffer> taskRecord, T parsedContent);

  @Override
  public int countQueueingTasks() {
    return taskQueue.size() + pausedTask.size() + inflight.get();
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }


  protected void next(KafkaConsumerRecord<String, Buffer> taskRecord) {
    var key = taskRecord.key();
    if (key!=null)
      lockingTaskKeys.remove(key);
    inflight.decrementAndGet();
    if (onRecordCompleteHandler!=null)
      onRecordCompleteHandler.accept(taskRecord);
    if (key!=null && pausedTask.containsKey(key)) {
      var col = pausedTask.get(key);
      if (!col.isEmpty()) {
        var rec = col.getFirst();
        pausedTask.remove(key, rec);
        taskQueue.offer(rec);
      }
    }
    consume();
  }

  @Override
  public Uni<Void> asyncStart() {
    logger.info("starting task invoker verticle [{}]", name);
    return super.asyncStart();
  }

  @Override
  public Uni<Void> asyncStop() {
    logger.info("stopping task invoker verticle [{}]", name);
    var interval = 500;
    return Multi.createFrom()
      .ticks()
      .every(Duration.ofMillis(interval))
      .filter(l -> {
        logger.info("{} ms waiting {} tasks for closing OrderedTaskInvoker[{}]",
          l * interval, countQueueingTasks(), name);
        return taskQueue.isEmpty() && pausedTask.isEmpty();
      })
      .toUni()
      .replaceWithVoid();
  }

}
