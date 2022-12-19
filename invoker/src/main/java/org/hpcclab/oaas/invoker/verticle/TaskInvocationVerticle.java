package org.hpcclab.oaas.invoker.verticle;

import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.Objects;
import java.util.Set;

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
  KafkaClientOptions options;
  @Inject
  MeterRegistry registry;
  Set<String> topics;

  private KafkaConsumer<String, Buffer> kafkaConsumer;
  private TaskMessageConsumer taskMessageConsumer;

  @Override
  public void init(Vertx vertx, Context context) {
    if (topics == null || topics.isEmpty()) {
      throw new IllegalStateException("topics must not be null or empty");
    }
    kafkaConsumer = KafkaConsumer.create(
      io.vertx.mutiny.core.Vertx.newInstance(vertx),
      options);
    taskMessageConsumer = new TaskMessageConsumer(
      invoker,
      funcRepo,
      graphExecutor,
      objCompPublisher,
      kafkaConsumer,
      topics,
      registry
    );
  }
  public void  setTopics(Set<String> topics) {
    this.topics = topics;
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
