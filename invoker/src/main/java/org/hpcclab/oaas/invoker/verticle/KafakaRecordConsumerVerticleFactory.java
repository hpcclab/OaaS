package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.lookup.HashRegistry;
import org.hpcclab.oaas.invoker.mq.OffsetManager;
import org.hpcclab.oaas.invoker.dispatcher.VerticlePoolRecordDispatcher;
import org.hpcclab.oaas.invoker.ispn.SegmentCoordinator;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class KafakaRecordConsumerVerticleFactory implements VerticleFactory<KafkaRecordConsumerVerticle> {
  private static final Logger logger = LoggerFactory.getLogger(KafakaRecordConsumerVerticleFactory.class);
  final
  Instance<OrderedInvocationHandlerVerticle> orderedInvokerVerticleInstance;
  final
  Instance<LockingRecordHandlerVerticle> lockingInvokerVerticleInstance;
  final
  InvokerConfig config;
  final
  ObjectRepoManager objectRepoManager;
//  final
//  LocationRegistry registry;
  final HashRegistry registry;
  final
  Vertx vertx;

    public KafakaRecordConsumerVerticleFactory(
      Instance<OrderedInvocationHandlerVerticle> orderedInvokerVerticleInstance,
      Instance<LockingRecordHandlerVerticle> lockingInvokerVerticleInstance,
      InvokerConfig config, ObjectRepoManager objectRepoManager,
      HashRegistry registry, Vertx vertx) {
        this.orderedInvokerVerticleInstance = orderedInvokerVerticleInstance;
        this.lockingInvokerVerticleInstance = lockingInvokerVerticleInstance;
        this.config = config;
        this.objectRepoManager = objectRepoManager;
        this.registry = registry;
        this.vertx = vertx;
    }

    @Override
  public KafkaRecordConsumerVerticle createVerticle(OClass cls) {
    var consumer = KafkaConsumer.create(vertx, options(config, cls.getKey()),
      String.class, Buffer.class);
    var offsetManager = new OffsetManager(consumer);
    var dispatcher = new VerticlePoolRecordDispatcher<>(vertx, createVerticleFactory(),
      offsetManager, config);
    dispatcher.setName(cls.getKey());
    var segmentCoordinator = new SegmentCoordinator(
      cls,
      objectRepoManager,
      consumer,
      registry,
      config
    );
    return new KafkaRecordConsumerVerticle<>(segmentCoordinator, consumer, dispatcher, config);
  }

  private VerticleFactory<RecordConsumerVerticle<KafkaConsumerRecord<String, Buffer>>> createVerticleFactory() {
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
