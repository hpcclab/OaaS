package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import org.hpcclab.oaas.invoker.verticle.KafakaRecordConsumerVerticleFactory;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VerticleDeployer {
  private static final Logger logger = LoggerFactory.getLogger(VerticleDeployer.class);
  final ConcurrentHashMap<String, Set<Verticle>> verticleMap = new ConcurrentHashMap<>();
  final ProtoMapper protoMapper = new ProtoMapperImpl();
  final KafakaRecordConsumerVerticleFactory verticleFactory;
  final Vertx vertx;
  final InvokerConfig config;

  public VerticleDeployer(KafakaRecordConsumerVerticleFactory verticleFactory,
                          Vertx vertx,
                          InvokerConfig config) {
    this.verticleFactory = verticleFactory;
    this.vertx = vertx;
    this.config = config;
  }


  public Uni<Void> deployVerticleIfNew(ProtoOClass protoOClass) {

    return vertx.sharedData().getLocalLock("deployer")
      .flatMap(l -> {
        if (verticleMap.containsKey(protoOClass.getKey()) && !verticleMap.get(protoOClass.getKey()).isEmpty()) {
          return Uni.createFrom().nullItem();
        }
        int size = config.numOfVerticle();
        var options = new DeploymentOptions();
        var cls = protoMapper.fromProto(protoOClass);
        return deployVerticle(cls, options, size)
          .eventually(l::release);
      });
  }

  protected Uni<Void> deployVerticle(OClass cls,
                                     DeploymentOptions options,
                                     int size) {
    List<Verticle> verticles = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      verticles.addAll(verticleFactory.createVerticles(cls));
    }
    logger.debug("prepare verticles {} for deploying",
      verticles);
    verticleMap.computeIfAbsent(cls.getKey(), key -> new HashSet<>())
      .addAll(verticles);
    return Multi.createFrom().iterable(verticles)
      .onItem()
      .transformToUniAndConcatenate(ver -> vertx.deployVerticle(ver, options))
      .onFailure().retry().withBackOff(Duration.ofMillis(100)).atMost(3)
      .invoke(id -> logger.info("deploy verticle[id={}] for [{}] successfully",
        id, cls.getKey()))
      .collect()
      .last()
      .replaceWithVoid();
  }
}
