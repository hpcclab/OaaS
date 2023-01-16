package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.OffsetManager;
import org.hpcclab.oaas.invoker.TaskVerticlePoolDispatcher;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class TaskConsumerVerticleFactory implements VerticleFactory<TaskConsumerVerticle> {
  @Inject
  Instance<OrderedTaskInvokerVerticle> invokerVerticleInstance;
  @Inject
  InvokerConfig config;
  @Inject
  Vertx vertx;
  @Inject
  KafkaClientOptions options;

  @Override
  public TaskConsumerVerticle createVerticle(String function) {
    var consumer = kafkaConsumer(options);
    VerticleFactory<OrderedTaskInvokerVerticle> invokerVerticleFactory = f -> invokerVerticleInstance.get();
    var offsetManager = new OffsetManager(consumer);
    var dispatcher = new TaskVerticlePoolDispatcher(vertx, invokerVerticleFactory,
      offsetManager, config);
    dispatcher.setName(function);
    var verticle = new TaskConsumerVerticle(consumer, dispatcher, config);
    verticle.setTopics(Set.of(config.functionTopicPrefix() + function));
    return verticle;
  }

  public KafkaConsumer<String, Buffer> kafkaConsumer(KafkaClientOptions options) {
//    var opt = new KafkaClientOptions(options.toJson());
//    opt.setConfig("enable.auto.commit", "false");
    return KafkaConsumer.create(vertx, options);
  }
}
