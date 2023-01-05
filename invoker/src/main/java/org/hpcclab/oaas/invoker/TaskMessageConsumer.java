package org.hpcclab.oaas.invoker;


import com.arangodb.ArangoDBException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuple3;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.invocation.InvokingDetail;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.invocation.function.InvocationGraphExecutor;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RegisterForReflection(
  targets = {OaasTask.class, TaskCompletion.class},
  registerFullHierarchy = true
)
public class TaskMessageConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageConsumer.class);

  SyncInvoker invoker;
  FunctionRepository funcRepo;
  InvocationGraphExecutor graphExecutor;
  ObjectCompletionPublisher objCompPublisher;
  KafkaConsumer<String, Buffer> kafkaConsumer;

  Set<String> topics;

  private final MeterRegistry registry;
  private final Timer invokingTimer;
  private final Timer completionTimer;

  public TaskMessageConsumer(SyncInvoker invoker,
                             FunctionRepository funcRepo,
                             InvocationGraphExecutor graphExecutor,
                             ObjectCompletionPublisher objCompPublisher,
                             KafkaConsumer<String, Buffer> kafkaConsumer,
                             Set<String> topics,
                             MeterRegistry registry) {
    this.invoker = invoker;
    this.funcRepo = funcRepo;
    this.graphExecutor = graphExecutor;
    this.objCompPublisher = objCompPublisher;
    this.kafkaConsumer = kafkaConsumer;
    this.topics = topics;
    this.registry = registry;
    var name = topics.toArray()[0];
    this.invokingTimer = Timer.builder("invoking." + name)
      .publishPercentiles(0.5, 0.75, 0.95, 0.99)
      .register(this.registry);
    this.completionTimer = Timer.builder("completion." + name)
      .publishPercentiles(0.5, 0.75, 0.95, 0.99)
      .register(this.registry);

  }

  public Uni<Void> start() {
    if (LOGGER.isDebugEnabled()) {
      setHandlerDebug(kafkaConsumer);
    } else {
      setHandler(kafkaConsumer);
    }
    return kafkaConsumer.subscribe(topics);
  }

  public Uni<Void> cleanup() {
    return kafkaConsumer.close();
  }

  void setHandler(KafkaConsumer<String, Buffer> kafkaConsumer) {
    kafkaConsumer.toMulti()
      .onItem()
      .transformToUni(kafkaRecord -> invoke(kafkaRecord)
        .call(tuple -> handleComplete(tuple.getOne(), tuple.getTwo()))
      )
      .merge(1024)
      .onFailure(KafkaInvokeException.class)
      .recoverWithItem(this::handleFailInvocation)
      .subscribe()
      .with(
        item -> {
          if (item!=null) objCompPublisher.publish(item.getTwo().getId());
        },
        error -> LOGGER.error("multi error", error),
        () -> LOGGER.error("multi unexpectedly completed")
      );
  }

  Pair<OaasTask, TaskCompletion> handleFailInvocation(Throwable exception) {
    if (exception instanceof KafkaInvokeException kafkaInvokeException) {
      var msg = kafkaInvokeException.getCause()!=null ? kafkaInvokeException
        .getCause().getMessage():null;
      LOGGER.warn("Catch invocation fail on '{}' with message '{}'", kafkaInvokeException.getTaskCompletion().getId(), msg);
      // TODO send to dead letter topic
    }
    return null;
  }

  void setHandlerDebug(KafkaConsumer<String, Buffer> kafkaConsumer) {
    kafkaConsumer.toMulti()
      .onItem()
      .transformToUni(kafkaConsumerRecord -> {
        var ts = System.currentTimeMillis();
        return invoke(kafkaConsumerRecord)
          .map(tuple -> Tuples.triple(tuple.getOne(), tuple.getTwo(), ts))
          .call(tuple -> {
              var ts2 = System.currentTimeMillis();
              return handleComplete(tuple.getOne(), tuple.getTwo())
                .invoke(() -> LOGGER.debug("task[{}]: persisted completion in {} ms (from start {} ms)",
                  tuple.getTwo().getId(),
                  System.currentTimeMillis() - ts2,
                  System.currentTimeMillis() - tuple.getThree()));
            }
          );
      })
      .merge(1024)
      .subscribe()
      .with(tuple -> {
          if (tuple==null) return;
          objCompPublisher.publish(tuple.getTwo().getId());
          LOGGER.debug("task[{}]: completed in {} ms",
            tuple.getTwo().getId(),
            System.currentTimeMillis() - tuple.getThree());
        },
        error -> LOGGER.error("multi error", error),
        () -> LOGGER.error("multi unexpectedly completed"));
  }

  Uni<Void> handleComplete(OaasTask task,
                           TaskCompletion completion) {
    try {
      var invokingTime = completion.getCptTs() - completion.getSmtTs();
      invokingTimer.record(invokingTime, TimeUnit.MILLISECONDS);
      var ts = System.currentTimeMillis();
      return graphExecutor.complete(task, completion)
        .onFailure(ArangoDBException.class)
        .retry()
        .withBackOff(Duration.ofMillis(500))
        .atMost(3)
        .onFailure()
        .transform(e -> new KafkaInvokeException(e, completion))
        .eventually(() -> completionTimer.record(System.currentTimeMillis() - ts, TimeUnit.MILLISECONDS));
    } catch (DecodeException decodeException) {
      return Uni.createFrom().failure(new KafkaInvokeException(decodeException, completion));
    }
  }

  Uni<Pair<OaasTask, TaskCompletion>> invoke(KafkaConsumerRecord<String, Buffer> kafkaRecord) {
    var startTime = System.currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      logLatency(kafkaRecord.value());
    }
    var funcName = kafkaRecord.headers()
      .stream()
      .filter(kafkaHeader -> kafkaHeader.key().equals("ce_function"))
      .findAny()
      .orElseThrow()
      .value()
      .toString();
    var id = kafkaRecord.headers()
      .stream()
      .filter(kafkaHeader -> kafkaHeader.key().equals("ce_id"))
      .findAny()
      .orElseThrow()
      .value()
      .toString();
    var task = Json.decodeValue(kafkaRecord.value(), OaasTask.class);

    return loadFuncUrl(funcName).flatMap(url -> {
      var invokingDetail = new InvokingDetail<Buffer>(
        id,
        task.getVId(),
        funcName,
        url,
        kafkaRecord.value(),
        System.currentTimeMillis()
      );
      var invokedUni = invoker.invoke(invokingDetail)
        .onFailure()
        .recoverWithItem(err -> TaskCompletion.error(
            invokingDetail.getId(),
            invokingDetail.getVId(),
            err.getMessage(),
            startTime,
            System.currentTimeMillis()
          )
        );

      if (LOGGER.isDebugEnabled()) {
        invokedUni = invokedUni.invoke(() -> LOGGER.debug(
          "task[{}]: invoked in {} ms", id, System.currentTimeMillis() - startTime));
      }

      return invokedUni
        .map(com -> Tuples.pair(task, com));
    });
  }

  Uni<String> loadFuncUrl(String funcName) {
    return funcRepo.getAsync(funcName)
      .flatMap(function -> {
        var cond = function.getDeploymentStatus().getCondition();
        if (cond==DeploymentCondition.DELETED) {
          return Uni.createFrom()
            .failure(new StdOaasException("Function was deleted"));
        } else if (cond==DeploymentCondition.RUNNING) {
          return Uni.createFrom()
            .item(function.getDeploymentStatus().getInvocationUrl());
        } else {
          return funcRepo.getWithoutCacheAsync(funcName)
            .map(func -> func.getDeploymentStatus().getInvocationUrl())
            .onItem().ifNull()
            .failWith(() -> new StdOaasException("Function is not ready"))
            .onFailure(StdOaasException.class)
            .retry().withBackOff(Duration.ofMillis(500))
            .expireIn(5000);
        }
      });
  }

  void logLatency(Buffer buffer) {
    OaasTask task = Json.decodeValue(buffer, OaasTask.class);
    var submittedTs = task.getTs();
    LOGGER.debug("task[{}]: Kafka latency {} ms", task.getTs(),
      System.currentTimeMillis() - submittedTs
    );
  }
}
