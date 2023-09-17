package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.OffsetManager;
import org.hpcclab.oaas.invoker.TaskVerticlePoolDispatcher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RecordConsumerVerticleFactory implements VerticleFactory<RecordConsumerVerticle> {
  private static final Logger logger = LoggerFactory.getLogger( RecordConsumerVerticleFactory.class );
  @Inject
  Instance<OrderedInvocationHandlerVerticle> orderedInvokerVerticleInstance;
  @Inject
  Instance<LockingRecordHandlerVerticle> lockingInvokerVerticleInstance;
  @Inject
  InvokerConfig config;
  @Inject
  Vertx vertx;

  @Override
  public RecordConsumerVerticle createVerticle(String suffix) {
    var consumer = KafkaConsumer.create(vertx, options(config, suffix),
      String.class, Buffer.class);
    var offsetManager = new OffsetManager(consumer);
    var dispatcher = new TaskVerticlePoolDispatcher(vertx,
            (VerticleFactory<? extends RecordHandlerVerticle<KafkaConsumerRecord>>) createVerticleFactory(),
      offsetManager, config);
    dispatcher.setName(suffix);
    var verticle = new RecordConsumerVerticle(consumer, dispatcher, config);
    verticle.setTopics(Set.of(config.invokeTopicPrefix() + suffix));
    return verticle;
  }

  VerticleFactory<?> createVerticleFactory() {
    if (config.clusterLock()) {
      logger.warn("The experimental 'Cluster lock' is enabled. LockingRecordHandlerVerticle will be used.");
      return f -> lockingInvokerVerticleInstance.get();
    } else {
      return f -> orderedInvokerVerticleInstance.get();
    }
  }


  public KafkaClientOptions options(InvokerConfig invokerConfig, String suffix) {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, invokerConfig.kafka());
    configMap.put(ConsumerConfig.GROUP_ID_CONFIG, invokerConfig.kafkaGroup() + "-" + suffix);
    configMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configMap.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1");
    configMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    return new KafkaClientOptions()
      .setConfig(configMap);
  }
}
