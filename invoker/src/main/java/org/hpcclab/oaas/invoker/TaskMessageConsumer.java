package org.hpcclab.oaas.invoker;


import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
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
import java.util.regex.Pattern;

public class TaskMessageConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageConsumer.class);

  SyncInvoker invoker;
  FunctionRepository funcRepo;
  InvocationGraphExecutor graphExecutor;
  ObjectCompletionPublisher objCompPublisher;
  KafkaConsumer<Buffer, Buffer> kafkaConsumer;

  Set<String> topics;

  public TaskMessageConsumer(SyncInvoker invoker,
                             FunctionRepository funcRepo,
                             InvocationGraphExecutor graphExecutor,
                             ObjectCompletionPublisher objCompPublisher,
                             KafkaConsumer<Buffer, Buffer> kafkaConsumer,
                             Set<String> topics) {
    this.invoker = invoker;
    this.funcRepo = funcRepo;
    this.graphExecutor = graphExecutor;
    this.objCompPublisher = objCompPublisher;
    this.kafkaConsumer = kafkaConsumer;
    this.topics = topics;
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

  void setHandler(KafkaConsumer<Buffer, Buffer> kafkaConsumer) {
    kafkaConsumer.toMulti()
      .onItem()
      .transformToUni(record -> invoke(record)
          .call(taskCompletion -> graphExecutor.complete(taskCompletion)
          .onFailure()
          .retry().withBackOff(Duration.ofMillis(500))
          .atMost(3)
          )
      )
      .merge(4096)
      .subscribe()
      .with(item -> {
          objCompPublisher.publish(item.getId());
        },
        error -> {
          LOGGER.error("multi error", error);
        },
        () -> {
          LOGGER.error("multi unexpectedly completed");
        });
  }

  void setHandlerDebug(KafkaConsumer<Buffer, Buffer> kafkaConsumer) {
    kafkaConsumer.toMulti()
      .onItem()
      .transformToUni(kafkaConsumerRecord -> {
        var ts = System.currentTimeMillis();
        var uni = invoke(kafkaConsumerRecord)
          .map(tc -> Tuples.pair(tc, ts))
          .call(tuple -> {
              var ts2 = System.currentTimeMillis();
              return graphExecutor.complete(tuple.getOne())
                .onFailure()
                .retry().withBackOff(Duration.ofMillis(500))
                .atMost(3)
                .invoke(() -> {
                  LOGGER.debug("task[{}]: persisted completion in {} ms (from start {} ms)",
                    tuple.getOne().getId(),
                    System.currentTimeMillis() - ts2,
                    System.currentTimeMillis() - tuple.getTwo());
                });
            }
          );
//        return Vertx.currentContext().executeBlocking(uni, false);
        return uni;
      })
      .merge(4096)
      .subscribe()
      .with(tuple -> {
          objCompPublisher.publish(tuple.getOne().getId());
          LOGGER.debug("task[{}]: completed in {} ms",
            tuple.getOne().getId(),
            System.currentTimeMillis() - tuple.getTwo());
        },
        error -> {
          LOGGER.error("multi error", error);
        },
        () -> {
          LOGGER.error("multi unexpectedly completed");
        });
  }

  Uni<TaskCompletion> invoke(KafkaConsumerRecord<Buffer, Buffer> record) {
    var startTime = System.currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      logLatency(record.value());
    }
    var funcName = record.headers()
      .stream()
      .filter(kafkaHeader -> kafkaHeader.key().equals("ce_function"))
      .findAny()
      .orElseThrow()
      .value()
      .toString();
    var id = record.headers()
      .stream()
      .filter(kafkaHeader -> kafkaHeader.key().equals("ce_id"))
      .findAny()
      .orElseThrow()
      .value()
      .toString();

    return loadFuncUrl(funcName).flatMap(url -> {
      var invokingDetail = new InvokingDetail<Buffer>(
        id,
        funcName,
        url,
        record.value()
      );
      var invokedUni = invoker.invoke(invokingDetail)
        .onFailure()
        .recoverWithItem(err -> new TaskCompletion(
            invokingDetail.getId(),
            false,
            err.getMessage(),
            null
          )
        );

      if (LOGGER.isDebugEnabled()) {
        invokedUni = invokedUni.invoke(() -> {
          LOGGER.debug("task[{}]: invoked in {} ms", id,
            System.currentTimeMillis() - startTime);
        });
      }
      return invokedUni;
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
