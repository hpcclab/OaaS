package org.hpcclab.oaas.invoker.verticle;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.invocation.function.InvocationGraphExecutor;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.TaskMessageConsumer;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class TaskInvocationVerticle extends AbstractVerticle {

  @Inject
  SyncInvoker invoker;
  @Inject
  FunctionRepository funcRepo;
  @Inject
  InvocationGraphExecutor graphExecutor;
  @Inject
  ObjectCompletionPublisher objCompPublisher;
  @Inject
  InvokerConfig config;
  @Inject
  KafkaClientOptions options;

  private KafkaConsumer<Buffer, Buffer> kafkaConsumer;
  private TaskMessageConsumer taskMessageConsumer;

  @Override
  public void init(Vertx vertx, Context context) {
    kafkaConsumer = KafkaConsumer.create(
      io.vertx.mutiny.core.Vertx.newInstance(vertx),
      options);
    taskMessageConsumer = new TaskMessageConsumer(
      invoker,
      funcRepo,
      graphExecutor,
      objCompPublisher,
      config,
      kafkaConsumer
    );
  }

  @Override
  public Uni<Void> asyncStart() {
    return taskMessageConsumer.start();
  }

  @Override
  public Uni<Void> asyncStop() {
    return taskMessageConsumer.cleanup();
  }
}
