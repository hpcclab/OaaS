package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.dispatcher.PartitionRecordDispatcher;
import org.hpcclab.oaas.invoker.ispn.SegmentCoordinator;
import org.hpcclab.oaas.invoker.lookup.HashRegistry;
import org.hpcclab.oaas.invoker.mq.OffsetManager;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class KafakaRecordConsumerVerticleFactory implements
  VerticleFactory<Verticle> {

  private static final Logger logger = LoggerFactory.getLogger(KafakaRecordConsumerVerticleFactory.class);

  final
  InvokerConfig config;
  final
  ObjectRepoManager objectRepoManager;
  final RecordHandlerVerticleFactory recordHandlerVerticleFactory;
  final HashRegistry registry;
  final Vertx vertx;

  public KafakaRecordConsumerVerticleFactory(
    InvokerConfig config,
    ObjectRepoManager objectRepoManager,
    RecordHandlerVerticleFactory recordHandlerVerticleFactory,
    HashRegistry registry,
    Vertx vertx) {

    this.config = config;
    this.objectRepoManager = objectRepoManager;
    this.recordHandlerVerticleFactory = recordHandlerVerticleFactory;
    this.registry = registry;
    this.vertx = vertx;
  }

  @Override
  public List<Verticle> createVerticles(OClass cls) {
    var consumer = KafkaConsumer.create(vertx, options(config, cls.getKey()),
      String.class, Buffer.class);
    var offsetManager = new OffsetManager(consumer);
    List<RecordHandlerVerticle> handlers = recordHandlerVerticleFactory.createVerticles(cls);
    var dispatcher = new PartitionRecordDispatcher(
      handlers,
      config
    );
    var segmentCoordinator = new SegmentCoordinator(
      cls,
      objectRepoManager,
      consumer,
      registry,
      config
    );
    List<Verticle> verticles = new ArrayList<>(handlers);
    verticles.add(new KafkaRecordConsumerVerticle(segmentCoordinator, consumer, offsetManager, dispatcher));
    return verticles;
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
