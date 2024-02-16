package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.kafka.admin.NewTopic;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.admin.KafkaAdminClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VerticleDeployer {
  private static final Logger logger = LoggerFactory.getLogger(VerticleDeployer.class);
  final ConcurrentHashMap<String, Set<AbstractVerticle>> verticleMap = new ConcurrentHashMap<>();
  final ProtoMapper protoMapper = new ProtoMapperImpl();
  VerticleFactory<?> verticleFactory;
  Vertx vertx;
  InvokerConfig config;
  KafkaAdminClient adminClient;

  public VerticleDeployer(VerticleFactory<?> verticleFactory,
                          Vertx vertx,
                          InvokerConfig config,
                          KafkaAdminClient adminClient) {
    this.verticleFactory = verticleFactory;
    this.vertx = vertx;
    this.config = config;
    this.adminClient = adminClient;
  }


  public Uni<Void> handleCls(ProtoOClass cls) {
    return createTopic(cls)
      .flatMap(v -> deployVerticleIfNew(cls));
  }

  Uni<Void> createTopic(OClass cls) {
    var topicName = config.invokeTopicPrefix() + cls.getKey();
    return adminClient.listTopics()
      .flatMap(topics -> {
        var topicExist = topics.contains(topicName);
        if (!topicExist) {
          var conf = cls.getConfig();
          return adminClient.createTopics(List.of(
            new NewTopic(topicName,
              conf==null ? OClassConfig.DEFAULT_PARTITIONS:conf.getPartitions(),
              (short) 1)
          ));
        }
        return Uni.createFrom().nullItem();
      });
  }

  Uni<Void> createTopic(ProtoOClass cls) {
    var topicName = config.invokeTopicPrefix() + cls.getKey();
    return adminClient.listTopics()
      .flatMap(topics -> {
        var topicExist = topics.contains(topicName);
        if (!topicExist) {
          var conf = cls.getConfig();
          return adminClient.createTopics(List.of(
            new NewTopic(topicName,
              conf.getPartitions() <= 0 ? OClassConfig.DEFAULT_PARTITIONS:conf.getPartitions(),
              (short) 1)
          ));
        }
        return Uni.createFrom().nullItem();
      });
  }


  public Uni<Void> deployVerticleIfNew(OClass cls) {
    if (verticleMap.containsKey(cls.getKey()) && !verticleMap.get(cls.getKey()).isEmpty()) {
      return Uni.createFrom().nullItem();
    }
    int size = config.numOfVerticle();
    var options = new DeploymentOptions();

    return deployVerticle(cls, options, size);
  }

  public Uni<Void> deployVerticleIfNew(ProtoOClass protoOClass) {
    if (verticleMap.containsKey(protoOClass.getKey()) && !verticleMap.get(protoOClass.getKey()).isEmpty()) {
      return Uni.createFrom().nullItem();
    }
    int size = config.numOfVerticle();
    var options = new DeploymentOptions();
    var cls = protoMapper.fromProto(protoOClass);
    return deployVerticle(cls, options, size);
  }

  protected Uni<Void> deployVerticle(OClass cls,
                                     DeploymentOptions options,
                                     int size) {
    return vertx
      .deployVerticle(() -> {
          AbstractVerticle vert = (AbstractVerticle) verticleFactory.createVerticle(cls);
          verticleMap.computeIfAbsent(cls.getKey(), key -> new HashSet<>())
            .add(vert);
          return vert;
        },
        options)
      .onFailure().retry().withBackOff(Duration.ofMillis(100)).atMost(3)
      .repeat().atMost(size)
      .invoke(id -> logger.info("deploy verticle[id={}] for [{}] successfully",
        id, cls.getKey()))
      .collect()
      .last()
      .replaceWithVoid();
  }

  public Uni<Void> deleteVerticle(OClass cls) {
    var verticleSet = verticleMap.get(cls.getKey());
    if (verticleSet!=null) {
      return Multi.createFrom().iterable(verticleSet)
        .call(vert -> vertx.undeploy(vert.deploymentID()))
        .invoke(id -> logger.info("Undeploy verticle[id={}] for [{}] successfully", id, cls.getKey()))
        .collect().last()
        .replaceWithVoid()
        .invoke(() -> verticleMap.remove(cls.getKey()));
    }
    return Uni.createFrom().nullItem();
  }

  public Map<String, Set<AbstractVerticle>> getVerticleIds() {
    return verticleMap;
  }
}
