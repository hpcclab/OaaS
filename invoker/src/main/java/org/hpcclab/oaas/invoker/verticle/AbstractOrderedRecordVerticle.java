package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.impl.ConcurrentHashSet;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.hpcclab.oaas.invoker.dispatcher.InvocationReqHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class AbstractOrderedRecordVerticle<T> extends AbstractVerticle
  implements RecordHandlerVerticle {

  private static final Logger logger = LoggerFactory.getLogger(AbstractOrderedRecordVerticle.class);
  final AtomicInteger inflight = new AtomicInteger(0);
  final AtomicBoolean lock = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<InvocationReqHolder> incomingQueue;
  private final ConcurrentLinkedQueue<InvocationReqHolder> waitingQueue;
  private final MutableListMultimap<String, InvocationReqHolder> pausedTask;
  private final ConcurrentHashSet<String> lockingTaskKeys = new ConcurrentHashSet<>();
  private final int maxConcurrent;
  protected Consumer<InvocationReqHolder> onRecordCompleteHandler;
  protected String name = "unknown";

  protected AbstractOrderedRecordVerticle(int maxConcurrent) {
    this.maxConcurrent = maxConcurrent;
    this.incomingQueue = new ConcurrentLinkedQueue<>();
    this.waitingQueue = new ConcurrentLinkedQueue<>();
    this.pausedTask = Multimaps.mutable.list.empty();
  }

  @Override
  public void setOnRecordCompleteHandler(Consumer<InvocationReqHolder> onRecordCompleteHandler) {
    this.onRecordCompleteHandler = onRecordCompleteHandler;
  }

  @Override
  public void offer(InvocationReqHolder taskRecord) {
    incomingQueue.offer(taskRecord);
    if (lock.get())
      return;
    context.runOnContext(__ -> consume());
  }

  protected abstract boolean shouldLock(InvocationReqHolder reqHolder);

  private void consume() {
    logger.debug("{}: consuming[lock={}, inflight={}]", name, lock, inflight);
    if (!lock.compareAndSet(false, true)) {
      return;
    }
    while (inflight.get() < maxConcurrent) {
      var taskRecord = waitingQueue.poll();
      if (taskRecord==null) {
        taskRecord = incomingQueue.poll();
      }
      if (taskRecord==null) {
        break;
      }
      if (taskRecord.key()==null) {
        inflight.incrementAndGet();
        handleRecord(taskRecord);
      } else if (lockingTaskKeys.contains(taskRecord.key())) {
        pausedTask.put(taskRecord.key(), taskRecord);
      } else {
        inflight.incrementAndGet();
        if (shouldLock(taskRecord)) {
          lockingTaskKeys.add(taskRecord.key());
        }
        handleRecord(taskRecord);
      }
    }

    lock.set(false);
  }

  protected abstract void handleRecord(InvocationReqHolder reqHolder);

  @Override
  public int countPending() {
    return incomingQueue.size() + pausedTask.size() + inflight.get();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  protected void next(InvocationReqHolder reqHolder) {
    var key = reqHolder.key();
    if (key!=null)
      lockingTaskKeys.remove(key);
    inflight.decrementAndGet();
    if (onRecordCompleteHandler!=null)
      onRecordCompleteHandler.accept(reqHolder);
    if (key!=null && pausedTask.containsKey(key)) {
      var queue = pausedTask.get(key);
      if (!queue.isEmpty()) {
        var rec = queue.getFirst();
        pausedTask.remove(key, rec);
        waitingQueue.add(rec);
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
          l * interval, countPending(), name);
        return incomingQueue.isEmpty() && pausedTask.isEmpty();
      })
      .toUni()
      .replaceWithVoid();
  }

}
