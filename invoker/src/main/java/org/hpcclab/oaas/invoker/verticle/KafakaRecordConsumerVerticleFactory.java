package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.OffsetManager;
import org.hpcclab.oaas.invoker.dispatcher.VerticlePoolRecordDispatcher;
import org.hpcclab.oaas.invoker.ispn.SegmentCoordinator;
import org.hpcclab.oaas.invoker.ispn.lookup.LocationRegistry;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class KafakaRecordConsumerVerticleFactory implements VerticleFactory<KafkaRecordConsumerVerticle> {
  private static final Logger logger = LoggerFactory.getLogger(KafakaRecordConsumerVerticleFactory.class);
  @Inject
  Instance<OrderedInvocationHandlerVerticle> orderedInvokerVerticleInstance;
  @Inject
  Instance<LockingRecordHandlerVerticle> lockingInvokerVerticleInstance;
  @Inject
  InvokerConfig config;
  @Inject
  ObjectRepoManager objectRepoManager;
  @Inject
  LocationRegistry registry;
  @Inject
  Vertx vertx;

  @Override
  public KafkaRecordConsumerVerticle createVerticle(OaasClass cls) {
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
    var verticle = new KafkaRecordConsumerVerticle<>(segmentCoordinator, consumer, dispatcher, config);
    return verticle;
  }

  VerticleFactory<RecordConsumerVerticle<KafkaConsumerRecord<String, Buffer>>> createVerticleFactory() {
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
