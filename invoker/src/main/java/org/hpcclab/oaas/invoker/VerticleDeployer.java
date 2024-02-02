package org.hpcclab.oaas.invoker;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.StartupEvent;
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
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnClsRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnFnRepository;
import org.hpcclab.oaas.invoker.mq.ClassListener;
import org.hpcclab.oaas.invoker.mq.FunctionListener;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.proto.ClassServiceGrpc;
import org.hpcclab.oaas.proto.OrbitStateServiceGrpc;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.SingleKeyQuery;
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
  @GrpcClient("package-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub orbitStateService;
  @GrpcClient("package-manager")
  ClassServiceGrpc.ClassServiceBlockingStub classService;
  final ProtoMapper protoMapper = new ProtoMapperImpl();


  void init(@Observes StartupEvent event) {
    deployPerCls();
    clsListener.setHandler(cls -> {
      logger.info("receive cls[{}] update event", cls.getKey());
      clsRepo.getCache().putForExternalRead(cls.getKey(), cls);
    });
    clsListener.start().await().indefinitely();
    functionListener.setHandler(fn -> {
      logger.info("receive fn[{}] update event", fn.getKey());
      fnRepo.getCache().putForExternalRead(fn.getKey(), fn);
    });
    clsListener.start().await().indefinitely();
  }

  void deployPerCls() {
    var orbitId = ConfigProvider.getConfig()
      .getValue("oprc.orbit", String.class);
    logger.info("loading orbit [id={}]", orbitId);
    var orbit = orbitStateService.get(SingleKeyQuery.newBuilder().setKey(orbitId).build());
    logger.info("handle orbit [id={}, cls={}, fn={}]",
      orbit.getId(), orbit.getAttachedClsList(), orbit.getAttachedFnList());
    var clsList = orbit.getAttachedClsList();
    for (var clsKey : clsList) {
      var cls = classService.get(SingleKeyQuery.newBuilder().setKey(clsKey).build());
      handleCls(cls);
    }
  }

  void handleCls(ProtoOClass cls) {
    createTopic(cls)
      .flatMap(__ -> deployVerticleIfNew(cls))
      .subscribe().with(
        __ -> {},
        e -> logger.error("Cannot deploy verticle for [{}]", cls.getKey(), e)
      );
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
              conf.getPartitions() <=0 ? OClassConfig.DEFAULT_PARTITIONS:conf.getPartitions(),
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
      .invoke(id -> {
        logger.info("deploy verticle[id={}] for [{}] successfully",
          id, cls.getKey());
      })
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
