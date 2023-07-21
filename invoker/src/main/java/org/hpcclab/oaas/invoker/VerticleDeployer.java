package org.hpcclab.oaas.invoker;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.invoker.verticle.VerticleFactory;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.FunctionState;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.HashSet;
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
  @Inject
  FunctionRepository funcRepo;
  @Inject
  FunctionListener functionListener;
  @Inject
  ClassRepository clsRepo;
  @Inject
  ClassListener clsListener;
  @Inject
  VerticleFactory<?> verticleFactory;
  @Inject
  Vertx vertx;
  @Inject
  InvokerConfig config;

  final ConcurrentHashMap<String, Set<AbstractVerticle>> verticleMap = new ConcurrentHashMap<>();

  void init(@Observes StartupEvent event) {
    deployPerCls();
  }

  void deployPerFunc() {
    var funcList = funcRepo.values()
      .select().first(1000)
      .collect().asList().await().indefinitely();

    functionListener.setHandler(func -> {
      LOGGER.info("receive func[{}] update event", func.getKey());
      funcRepo.delete(func.getKey());
      handleFunc(func);
    });
    functionListener.start().await().indefinitely();

    for (var func : funcList) {
      handleFunc(func);
    }
  }

  void deployPerCls() {
    var clsList = clsRepo.values()
      .select().first(1000)
      .collect().asList().await().indefinitely();

    clsListener.setHandler(cls -> {
      LOGGER.info("receive cls[{}] update event", cls.getKey());
      clsRepo.delete(cls.getKey());
      handleCls(cls);
    });
    clsListener.start().await().indefinitely();

    for (var cls : clsList) {
      handleCls(cls);
    }
  }


  void handleFunc(OaasFunction func) {
    if (func.getType()==FunctionType.LOGICAL)
      return;
    if (func.getState()==FunctionState.ENABLED) {
      deployVerticleIfNew(func.getKey())
        .subscribe().with(
          __ -> {},
          e -> LOGGER.error("Cannot deploy verticle for [{}]", func.getKey(), e)
        );
    } else if (func.getState()==FunctionState.REMOVING || func.getState()==FunctionState.DISABLED) {
      deleteVerticle(func.getKey())
        .subscribe().with(
          __ -> { },
          e -> LOGGER.error("Cannot delete verticle for [{}]", func.getKey(), e)
        );
    }
  }

  void handleCls(OaasClass cls) {
    if (!cls.isMarkForRemoval()) {
      deployVerticleIfNew(cls.getKey())
        .subscribe().with(
          __ -> {},
          e -> LOGGER.error("Cannot deploy verticle for [{}]", cls.getKey(), e)
        );
    } else {
      LOGGER.info("deleting {}", cls.getKey());
      deleteVerticle(cls.getKey())
        .subscribe().with(
          __ -> {},
          e -> LOGGER.error("Cannot delete verticle for [{}]", cls.getKey(), e)
        );
    }
  }

  void cleanup(@Observes ShutdownEvent event) {
    Multi.createFrom().iterable(verticleMap.values())
      .flatMap(set -> Multi.createFrom().iterable(set))
      .call(vert -> vertx.undeploy(vert.deploymentID()))
      .collect()
      .asList()
      .replaceWithVoid()
      .await().indefinitely();
  }

  public Uni<Void> deployVerticleIfNew(String function) {
    if (verticleMap.containsKey(function) && !verticleMap.get(function).isEmpty()) {
      return Uni.createFrom().nullItem();
    }
    int size = config.numOfVerticle();
    var options = new DeploymentOptions();

    return deployVerticle(function, options, size);
  }

  protected Uni<Void> deployVerticle(String suffix,
                                     DeploymentOptions options,
                                     int size) {
    return vertx
      .deployVerticle(() -> {
          AbstractVerticle vert = (AbstractVerticle) verticleFactory.createVerticle(suffix);
          verticleMap.computeIfAbsent(suffix, key -> new HashSet<>())
            .add(vert);
          return vert;
        },
        options)
      .repeat().atMost(size)
      .invoke(id -> {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("deploy verticle[id={}] for {} successfully",
            id, suffix);
//          LOGGER.info("verticles {}", verticleMap.keySet().stream().toList());
        }
      })
      .collect()
      .last()
      .replaceWithVoid();
  }

  public Uni<Void> deleteVerticle(String function) {
    var verticleSet = verticleMap.get(function);
    if (verticleSet!=null) {
      return Multi.createFrom().iterable(verticleSet)
        .call(vert -> vertx.undeploy(vert.deploymentID()))
        .invoke(id -> LOGGER.info("Undeploy verticle[id={}] for func {} successfully", id, function))
        .collect().last()
        .replaceWithVoid()
        .invoke(() -> verticleMap.remove(function));
    }
    return Uni.createFrom().nullItem();
  }

  public Map<String, Set<AbstractVerticle>> getVerticleIds() {
    return verticleMap;
  }
}
