package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.dispatcher.InvocationReqHolder;
import org.hpcclab.oaas.invoker.service.InvocationRecordHandler;
import org.infinispan.lock.api.ClusteredLockConfiguration;
import org.infinispan.lock.api.ClusteredLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Dependent
public class LockingRecordHandlerVerticle extends AbstractVerticle
  implements RecordHandlerVerticle {

  private static final Logger logger = LoggerFactory.getLogger(LockingRecordHandlerVerticle.class);
  final AtomicInteger acquireCounter = new AtomicInteger(0);
  final AtomicInteger inflightCounter = new AtomicInteger(0);
  final InvocationRecordHandler invocationRecordHandler;
  final ConcurrentLinkedQueue<InvocationReqHolder> taskQueue;
  final ClusteredLockManager lockManager;
  private final int maxConcurrent;
  Consumer<InvocationReqHolder> onRecordCompleteHandler;
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
  public void offer(InvocationReqHolder taskRecord) {
    taskQueue.add(taskRecord);
    if (inflightCounter.get() < maxConcurrent) {
      context.runOnContext(__ -> consume());
    }
  }

  private void consume() {
    if (inflightCounter.get() > maxConcurrent) {
      return;
    }
    var taskRecord = taskQueue.poll();
    if (taskRecord == null)
      return;
    acquireCounter.incrementAndGet();
    var req = taskRecord.getReq();
    if (req.immutable()) {
      invocationRecordHandler.handleRecord(
        taskRecord,
        this::complete,
        true
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
            inflightCounter.incrementAndGet();
            invocationRecordHandler.handleRecord(
              taskRecord,
              rec -> {
                lock.unlock();
                complete(taskRecord);
              },
              true
            );
          } else {
            taskQueue.offer(taskRecord);
            acquireCounter.decrementAndGet();
          }
        });
    }
  }


  protected void complete(InvocationReqHolder reqHolder) {
    if (onRecordCompleteHandler!=null)
      onRecordCompleteHandler.accept(reqHolder);
    acquireCounter.decrementAndGet();
    inflightCounter.decrementAndGet();
    consume();
  }

  @Override
  public int countPending() {
    return acquireCounter.get() + taskQueue.size();
  }

  @Override
  public void setOnRecordCompleteHandler(Consumer<InvocationReqHolder> onRecordCompleteHandler) {
    this.onRecordCompleteHandler = onRecordCompleteHandler;
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
