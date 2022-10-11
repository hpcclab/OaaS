package org.hpcclab.oaas.invoker;


import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.invocation.InvokingDetail;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.invocation.function.InvocationGraphExecutor;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class TaskMessageConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageConsumer.class);

  SyncInvoker invoker;
  FunctionRepository funcRepo;
  InvocationGraphExecutor graphExecutor;
  ObjectCompletionPublisher objCompPublisher;
  InvokerConfig config;
  KafkaConsumer<Buffer, Buffer> kafkaConsumer;

  public TaskMessageConsumer(SyncInvoker invoker,
                             FunctionRepository funcRepo,
                             InvocationGraphExecutor graphExecutor,
                             ObjectCompletionPublisher objCompPublisher,
                             InvokerConfig config,
                             KafkaConsumer<Buffer, Buffer> kafkaConsumer) {
    this.invoker = invoker;
    this.funcRepo = funcRepo;
    this.graphExecutor = graphExecutor;
    this.objCompPublisher = objCompPublisher;
    this.config = config;
    this.kafkaConsumer = kafkaConsumer;
  }

  public Uni<Void> start() {
    var topics = config.topics();
    setHandler(kafkaConsumer);
    return kafkaConsumer.subscribe(topics);
  }

  public Uni<Void> cleanup() {
    return kafkaConsumer.close();
  }

  void setHandler(KafkaConsumer<Buffer, Buffer> kafkaConsumer) {
    kafkaConsumer.toMulti()
      .onItem()
//      .transformToUni(this::invoke)
      .transformToUniAndMerge(this::invoke)
      .call(taskCompletion -> {
//        var starTime = System.currentTimeMillis();
        return graphExecutor.complete(taskCompletion)
            .onFailure()
            .retry().withBackOff(Duration.ofMillis(500))
            .atMost(3);
//          .invoke(() -> {
//            LOGGER.info("Persisted completion {} in {} ms",
//              taskCompletion.getId(),
//              System.currentTimeMillis() - starTime);
//          });
        }
      )
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

  Uni<TaskCompletion> invoke(KafkaConsumerRecord<Buffer, Buffer> record) {
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
    var function = funcRepo.get(funcName);

    Uni<String> uni;

    if (function.getDeploymentStatus().getCondition()==DeploymentCondition.DELETED) {
      return Uni.createFrom().item(new TaskCompletion(
          id,
          false,
          "Function was deleted",
          null
        )
      );
    } else if (function.getDeploymentStatus().getCondition()==DeploymentCondition.RUNNING) {
      uni = Uni.createFrom().item(function.getDeploymentStatus().getInvocationUrl());
    } else {
      uni = funcRepo.getWithoutCacheAsync(funcName)
        .map(func -> func.getDeploymentStatus().getInvocationUrl())
        .onItem().ifNull()
        .failWith(() -> new StdOaasException("Function is not ready"))
        .onFailure(StdOaasException.class)
        .retry().withBackOff(Duration.ofMillis(500))
        .expireIn(5000);
    }


    return uni.flatMap(url -> {
      var invokingDetail = new InvokingDetail<Buffer>(
        id,
        funcName,
        function.getDeploymentStatus().getInvocationUrl(),
        record.value()
      );
      var startTime = System.currentTimeMillis();
      return invoker.invoke(invokingDetail)
        .invoke(() -> {
          LOGGER.info("Invoked task {} done in {} ms", id,
            System.currentTimeMillis() - startTime);
        })
        .onFailure().recoverWithItem(err -> new TaskCompletion(
            invokingDetail.getId(),
            false,
            err.getMessage(),
            null
          )
        );
    });
  }
}
