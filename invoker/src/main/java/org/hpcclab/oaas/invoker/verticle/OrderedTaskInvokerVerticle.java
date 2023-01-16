package org.hpcclab.oaas.invoker.verticle;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.invocation.InvokingDetail;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.invocation.function.InvocationGraphExecutor;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.KafkaInvokeException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.hpcclab.oaas.invoker.TaskConsumer.extractId;

@Dependent
public class OrderedTaskInvokerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrderedTaskInvokerVerticle.class);
  final SyncInvoker invoker;
  final FunctionRepository funcRepo;
  final InvocationGraphExecutor graphExecutor;
  final ObjectCompletionPublisher objCompPublisher;
  final AtomicInteger inflight = new AtomicInteger(0);
  final AtomicBoolean lock = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<KafkaConsumerRecord<String, Buffer>> taskQueue
    = new ConcurrentLinkedQueue<>();
  private final MutableListMultimap<String, KafkaConsumerRecord<String, Buffer>> pausedTask
    = Multimaps.mutable.list.empty();
  private final ConcurrentHashSet<String> runningTaskKeys = new ConcurrentHashSet<>();
  private final int maxConcurrent;
  private final Timer invokingTimer;
  private final Timer completionTimer;
  Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler;
  String name = "unknown";

  @Inject
  public OrderedTaskInvokerVerticle(SyncInvoker invoker,
                                    FunctionRepository funcRepo,
                                    InvocationGraphExecutor graphExecutor,
                                    ObjectCompletionPublisher objCompPublisher,
                                    InvokerConfig invokerConfig,
                                    MeterRegistry registry) {
    this.maxConcurrent = invokerConfig.invokeConcurrency();
    this.invoker = invoker;
    this.funcRepo = funcRepo;
    this.graphExecutor = graphExecutor;
    this.objCompPublisher = objCompPublisher;
    if (registry!=null) {
      this.invokingTimer = Timer.builder("invoker." + name + ".invoking")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);
      this.completionTimer = Timer.builder("invoker." + name + ".completion")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);
    } else {
      this.invokingTimer = null;
      this.completionTimer = null;
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Uni<Void> asyncStart() {
    LOGGER.info("starting task invoker verticle [{}]", name);
    return super.asyncStart();
  }

  @Override
  public Uni<Void> asyncStop() {
    LOGGER.info("stopping task invoker verticle [{}]", name);
    var interval = 500;
    return Multi.createFrom()
      .ticks()
      .every(Duration.ofMillis(interval))
      .filter(l -> {
        LOGGER.info("{} ms waiting {} tasks for closing OrderedTaskInvoker[{}]",
          l * interval, countQueueingTasks(), name);
        return taskQueue.isEmpty() && pausedTask.isEmpty();
      })
      .toUni()
      .replaceWithVoid();
  }

  public int countQueueingTasks() {
    return taskQueue.size() + pausedTask.size();
  }

  public void setOnRecordCompleteHandler(Consumer<KafkaConsumerRecord<String, Buffer>> onRecordCompleteHandler) {
    this.onRecordCompleteHandler = onRecordCompleteHandler;
  }

  public void offer(KafkaConsumerRecord<String, Buffer> taskRecord) {
    taskQueue.offer(taskRecord);
    if (lock.get())
      return;
    context.runOnContext(__ -> consume());
  }

  public void consume() {
    LOGGER.debug("{}: consuming[lock={}, inflight={}]", name, lock, inflight);
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
        invokeRecord(taskRecord);
      } else if (runningTaskKeys.contains(taskRecord.key())) {
        pausedTask.put(taskRecord.key(), taskRecord);
      } else {
        inflight.incrementAndGet();
        runningTaskKeys.add(taskRecord.key());
        invokeRecord(taskRecord);
      }
    }

    lock.set(false);
  }

  public void next(KafkaConsumerRecord<String, Buffer> taskRecord) {
    var key = taskRecord.key();
    if (key!=null)
      runningTaskKeys.remove(key);
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

  public void invokeRecord(KafkaConsumerRecord<String, Buffer> taskRecord) {
    var startTime = System.currentTimeMillis();
    var id = extractId(taskRecord);
    var task = Json.decodeValue(taskRecord.value(), OaasTask.class);
    if (LOGGER.isDebugEnabled()) {
      logLatency(task);
    }
    generateInvokingDetail(taskRecord, task)
      .flatMap(invokingDetail -> {
        var invokedUni = invoker.invoke(invokingDetail)
          .onFailure()
          .recoverWithItem(err -> TaskCompletion.error(
              TaskIdentity.decode(invokingDetail.getId()),
              err.getMessage(),
              startTime,
              System.currentTimeMillis()
            )
          );

        if (LOGGER.isDebugEnabled()) {
          invokedUni = invokedUni.invoke(() -> LOGGER.debug(
            "{}: task[{}]: invoked in {} ms",
            name, id, System.currentTimeMillis() - startTime));
        }

        return invokedUni
          .map(com -> Tuples.pair(task, com));
      })
      .flatMap(this::handleComplete)
      .onFailure()
      .recoverWithItem(this::handleFailInvocation)
      .subscribe()
      .with(
        item -> {
          if (item!=null) {
            var taskId = item.getTwo().getId();
            objCompPublisher.publish(taskId.oId()==null ? taskId.mId():taskId.oId());
          }
          next(taskRecord);
        },
        error -> {
          LOGGER.error("Unexpected error on invoker ", error);
          next(taskRecord);
        }
      );
  }

  Uni<Pair<OaasTask, TaskCompletion>> handleComplete(Pair<OaasTask, TaskCompletion> pair) {
    var task = pair.getOne();
    var completion = pair.getTwo();
    try {
      var invokingTime = completion.getCptTs() - completion.getSmtTs();
      if (invokingTimer!=null)
        invokingTimer.record(invokingTime, TimeUnit.MILLISECONDS);
      var ts = System.currentTimeMillis();
      var uni = graphExecutor.complete(task, completion)
        .onFailure()
        .transform(e -> new KafkaInvokeException(e, completion))

        .replaceWith(pair);
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("{} persist task[{}] in {} ms",
          name,
          task.getId(),
          System.currentTimeMillis() - ts
        );
      if (completionTimer!=null)
        uni = uni.eventually(() ->
          completionTimer.record(System.currentTimeMillis() - ts, TimeUnit.MILLISECONDS));
      return uni;
    } catch (DecodeException decodeException) {
      return Uni.createFrom().failure(new KafkaInvokeException(decodeException, completion));
    }
  }

  Pair<OaasTask, TaskCompletion> handleFailInvocation(Throwable exception) {
    if (exception instanceof KafkaInvokeException kafkaInvokeException) {
      var msg = kafkaInvokeException.getCause()!=null ? kafkaInvokeException
        .getCause().getMessage():null;
      if (LOGGER.isWarnEnabled())
        LOGGER.warn("Catch invocation fail on '{}' with message '{}'",
          kafkaInvokeException.getTaskCompletion().getId().encode(),
          msg,
          kafkaInvokeException
        );
      // TODO send to dead letter topic
    }
    return null;
  }

  Uni<InvokingDetail<Buffer>> generateInvokingDetail(
    KafkaConsumerRecord<String, Buffer> rec,
    OaasTask task) {
    return funcRepo.getAsync(task.getFuncKey())
      .flatMap(function -> {
        var cond = function.getDeploymentStatus().getCondition();
        if (cond==DeploymentCondition.DELETED) {
          return Uni.createFrom()
            .failure(new StdOaasException("Function was deleted"));
        } else if (cond==DeploymentCondition.RUNNING) {
          return Uni.createFrom()
            .item(function);
        } else {
          return funcRepo.getWithoutCacheAsync(task.getFuncKey())
            .onItem().ifNull()
            .failWith(() -> new StdOaasException("Function is not ready"))
            .onFailure(StdOaasException.class)
            .retry().withBackOff(Duration.ofMillis(500))
            .expireIn(5000);
        }
      })
      .invoke(task::setFunction)
      .map(function -> new InvokingDetail<Buffer>(
        task.getId(),
        task.getVId(),
        task.getFuncKey(),
        function.getDeploymentStatus().getInvocationUrl(),
        rec.value(),
        System.currentTimeMillis()
      ));
  }

  void logLatency(OaasTask task) {
    var submittedTs = task.getTs();
    LOGGER.debug("{}: task[{}]: Kafka latency {} ms",
      name,
      task.getId(),
      System.currentTimeMillis() - submittedTs
    );
  }
}
