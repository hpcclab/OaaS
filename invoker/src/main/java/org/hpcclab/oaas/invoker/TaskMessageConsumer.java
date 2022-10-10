package org.hpcclab.oaas.invoker;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.hpcclab.oaas.invocation.InvokingDetail;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class TaskMessageConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageConsumer.class);

  @Inject
  public Vertx vertx;
  @Inject
  SyncInvoker invoker;
  @Inject
  FunctionRepository funcRepo;
  @Inject
  KafkaClientOptions options;
  @Inject
  InvokerConfig config;

  private KafkaConsumer<Buffer, Buffer> kafkaConsumer;


  public void setup(@Observes StartupEvent event) {
    kafkaConsumer = KafkaConsumer.create(vertx, options);
    var topics = config.topics();
    setHandler(kafkaConsumer);
    kafkaConsumer.subscribeAndAwait(topics);
    LOGGER.info("subscribe to kafka topics {} successfully", topics);
  }

  public void cleanup(@Observes ShutdownEvent event) {
    kafkaConsumer.closeAndAwait();
  }

  void setHandler(KafkaConsumer<Buffer, Buffer> kafkaConsumer) {
    kafkaConsumer.toMulti()
      .onItem()
//      .transformToUni(this::invoke)
      .transformToUniAndMerge(this::invoke)
      .subscribe()
      .with(item -> {
          LOGGER.info("process TaskCompletion {}", item);
        },
        error -> {
          LOGGER.error("multi error", error);
        },
        () -> {
          LOGGER.info("multi completed");
        });
  }

  Uni<TaskCompletion> mockInvoke(KafkaConsumerRecord<Buffer, Buffer> record) {
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
    LOGGER.info("receive record {} {}", funcName, id);
    return Uni.createFrom().item(new TaskCompletion(
      id,
      false,
      "mock",
      null
    ));
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
    var invokingDetail = new InvokingDetail<Buffer>(
      id,
      funcName,
      function.getDeploymentStatus().getInvocationUrl(),
      record.value()
    );
    LOGGER.info("invokingDetail {} {} {}", invokingDetail.getId(),
      invokingDetail.getFuncName(), invokingDetail.getFuncUrl());
    return invoker.invoke(invokingDetail)
      .onFailure().recoverWithItem(err -> new TaskCompletion(
          invokingDetail.getId(),
          false,
          err.getMessage(),
          null
        )
      );
  }
}
