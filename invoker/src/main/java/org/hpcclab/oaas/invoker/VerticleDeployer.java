package org.hpcclab.oaas.invoker;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.kafka.admin.NewTopic;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.admin.KafkaAdminClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnClsRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnFnRepository;
import org.hpcclab.oaas.invoker.mq.ClassListener;
import org.hpcclab.oaas.invoker.mq.FunctionListener;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;
import org.hpcclab.oaas.model.cls.ClassConfig;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@RegisterForReflection(
  targets = {
    OaasFunction.class
  },
  registerFullHierarchy = true
)
public class VerticleDeployer {
  private static final Logger LOGGER = LoggerFactory.getLogger(VerticleDeployer.class);
  final ConcurrentHashMap<String, Set<AbstractVerticle>> verticleMap = new ConcurrentHashMap<>();
  @Inject
  EIspnClsRepository clsRepo;
  @Inject
  EIspnFnRepository fnRepo;
  @Inject
  ClassListener clsListener;
  @Inject
  FunctionListener functionListener;
  @Inject
  VerticleFactory<?> verticleFactory;
  @Inject
  Vertx vertx;
  @Inject
  InvokerConfig config;
  @Inject
  KafkaAdminClient adminClient;

  void init(@Observes StartupEvent event) {
    deployPerCls();
    clsListener.setHandler(cls -> {
      LOGGER.info("receive cls[{}] update event", cls.getKey());
      clsRepo.getCache().putForExternalRead(cls.getKey(), cls);
      handleCls(cls);
    });
    clsListener.start().await().indefinitely();
    functionListener.setHandler(fn -> {
      LOGGER.info("receive fn[{}] update event", fn.getKey());
      fnRepo.getCache().putForExternalRead(fn.getKey(), fn);
    });
    clsListener.start().await().indefinitely();
  }

//  void cleanup(@Observes ShutdownEvent event) {
//    Multi.createFrom().iterable(verticleMap.values())
//      .flatMap(set -> Multi.createFrom().iterable(set))
//      .call(vert -> vertx.undeploy(vert.deploymentID()))
//      .collect()
//      .asList()
//      .replaceWithVoid()
//      .await().indefinitely();
//  }

  void deployPerCls() {
    var clsList = clsRepo.async()
      .values()
      .select().first(1000)
      .collect().asList().await().indefinitely();
    for (var cls : clsList) {
      handleCls(cls);
    }
  }


  void handleCls(OaasClass cls) {
    if (!cls.isMarkForRemoval()) {
      createTopic(cls)
        .flatMap(__ -> deployVerticleIfNew(cls))
        .subscribe().with(
          __ -> {
          },
          e -> LOGGER.error("Cannot deploy verticle for [{}]", cls.getKey(), e)
        );
    } else {
      LOGGER.info("deleting {}", cls.getKey());
      deleteVerticle(cls)
        .subscribe().with(
          __ -> {
          },
          e -> LOGGER.error("Cannot delete verticle for [{}]", cls.getKey(), e)
        );
    }
  }

  Uni<Void> createTopic(OaasClass cls) {
    var topicName = config.invokeTopicPrefix() + cls.getKey();
    return adminClient.listTopics()
      .flatMap(topics -> {
        var topicExist = topics.contains(topicName);
        if (!topicExist) {
          var conf = cls.getConfig();
          return adminClient.createTopics(List.of(
            new NewTopic(topicName,
              conf == null? ClassConfig.DEFAULT_PARTITIONS : conf.getPartitions(),
              (short) 1)
          ));
        }
        return Uni.createFrom().nullItem();
      });
  }


  public Uni<Void> deployVerticleIfNew(OaasClass cls) {
    if (verticleMap.containsKey(cls.getKey()) && !verticleMap.get(cls.getKey()).isEmpty()) {
      return Uni.createFrom().nullItem();
    }
    int size = config.numOfVerticle();
    var options = new DeploymentOptions();

    return deployVerticle(cls, options, size);
  }

  protected Uni<Void> deployVerticle(OaasClass cls,
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
      .invoke(id -> {
        LOGGER.info("deploy verticle[id={}] for [{}] successfully",
          id, cls.getKey());
      })
      .collect()
      .last()
      .replaceWithVoid();
  }

  public Uni<Void> deleteVerticle(OaasClass cls) {
    var verticleSet = verticleMap.get(cls.getKey());
    if (verticleSet!=null) {
      return Multi.createFrom().iterable(verticleSet)
        .call(vert -> vertx.undeploy(vert.deploymentID()))
        .invoke(id -> LOGGER.info("Undeploy verticle[id={}] for [{}] successfully", id, cls.getKey()))
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
