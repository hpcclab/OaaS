package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.InvocationRecordHandler;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.infinispan.lock.api.ClusteredLockConfiguration;
import org.infinispan.lock.api.ClusteredLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Dependent
public class LockingRecordHandlerVerticle extends AbstractVerticle implements RecordHandlerVerticle<KafkaConsumerRecord<String, Buffer>> {
  private static final Logger logger = LoggerFactory.getLogger(LockingRecordHandlerVerticle.class);
  final AtomicInteger inflightCounter = new AtomicInteger(0);
  final InvocationRecordHandler invocationRecordHandler;
  final ConcurrentLinkedQueue<KafkaConsumerRecord<String, Buffer>> taskQueue;
  final ClusteredLockManager lockManager;
  private final int maxConcurrent;
  Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler;
  String name = "unknown";

  @Inject
  public LockingRecordHandlerVerticle(InvokerConfig invokerConfig,
                                      InvocationRecordHandler invocationRecordHandler,
                                      ClusteredLockManager lockManager) {
    this.invocationRecordHandler = invocationRecordHandler;
    this.taskQueue = new ConcurrentLinkedQueue<>();
    this.maxConcurrent = invokerConfig.invokeConcurrency();
    this.lockManager = lockManager;
  }

  @Override
  public void setOnRecordCompleteHandler(Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler) {
    this.onRecordCompleteHandler = onRecordCompleteHandler;
  }

  @Override
  public void offer(KafkaConsumerRecord<String, Buffer> taskRecord) {
    taskQueue.add(taskRecord);
    if (inflightCounter.get() < maxConcurrent) {
      context.runOnContext(__ -> consume());
    }
  }

  private void consume() {
    if (inflightCounter.get() > maxConcurrent) {
      return;
    }
    if (taskQueue.isEmpty())
      return;
    inflightCounter.incrementAndGet();
    var taskRecord = taskQueue.poll();
    var req = Json.decodeValue(taskRecord.value(), InvocationRequest.class);
    if (req.immutable()) {
      invocationRecordHandler.handleRecord(
        taskRecord,
        req,
        this::complete,
        this::skipCondition
      );
    } else {
      var key = taskRecord.key();
      if (!lockManager.isDefined(key)) {
        lockManager.defineLock(key, new ClusteredLockConfiguration());
      }
      var lock = lockManager.get(key);
      lock.tryLock(3, TimeUnit.MINUTES)
        .thenAccept(locked -> {
          if (Boolean.TRUE.equals(locked)) {
            invocationRecordHandler.handleRecord(
              taskRecord, req,
              (rec, req2) -> {
                lock.unlock();
                complete(taskRecord, req2);
              },
              this::skipCondition
            );
          } else {
            taskQueue.offer(taskRecord);
            inflightCounter.decrementAndGet();
          }
        });
    }
  }

  boolean skipCondition(KafkaConsumerRecord<String, Buffer> taskRecord,
                        InvocationContext ctx) {
    return false;
  }

  protected void complete(KafkaConsumerRecord<String, Buffer> taskRecord,
                          InvocationRequest request) {
    if (onRecordCompleteHandler!=null)
      onRecordCompleteHandler.accept(taskRecord);
    inflightCounter.decrementAndGet();
    consume();
  }

  @Override
  public int countQueueingTasks() {
    return inflightCounter.get();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }
}
