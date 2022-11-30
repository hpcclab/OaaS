package org.hpcclab.oaas.invoker.verticle;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;
import org.hpcclab.oaas.invoker.FunctionListener;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@RegisterForReflection(
  targets = {
    OaasFunction.class
  },
  registerFullHierarchy = true
)
class VerticleDeployer {
  private static final Logger LOGGER = LoggerFactory.getLogger(VerticleDeployer.class);
  @Inject
  ArgRepositoryInitializer initializer;
  @Inject
  FunctionRepository funcRepo;
  @Inject
  Instance<TaskInvocationVerticle> verticles;
  @Inject
  Vertx vertx;
  @Inject
  InvokerConfig config;
  @Inject
  FunctionListener functionListener;


  private final ConcurrentHashMap<String, Set<String>> verticleIds = new ConcurrentHashMap<>();

  void init(@Observes StartupEvent ev) {
    initializer.setup();
    var funcPage = funcRepo.pagination(0, 1000);
    var funcList = funcPage.getItems();


    functionListener.setHandler(func -> {
      LOGGER.info("receive function[{}] update event", func.getKey());
      if (func.getState()==FunctionState.ENABLED) {
        deployVerticleIfNew(func.getKey())
          .subscribe().with(__ -> {
            },
            e -> LOGGER.error("Cannot deploy verticle for function {}", func.getKey()));
      } else if (func.getState()==FunctionState.REMOVING || func.getState()==FunctionState.DISABLED) {
        deleteVerticle(func.getKey())
          .subscribe().with(__ -> {
            },
            e -> LOGGER.error("Cannot delete verticle for function {}", func.getKey()));
      }
    });
    functionListener.start().await().indefinitely();

    Multi.createFrom().iterable(funcList)
      .filter(function -> function.getState()==FunctionState.ENABLED)
      .filter(function -> function.getType()!=FunctionType.LOGICAL)
      .onItem().transformToUniAndConcatenate(func -> deployVerticleIfNew(func.getKey()))
      .collect().asList()
      .await().indefinitely();
  }

  public Uni<String> deployVerticleIfNew(String function) {
    if (verticleIds.containsKey(function) && !verticleIds.get(function).isEmpty()) {
      return Uni.createFrom().nullItem();
    }
    int size = config.numOfVerticle();
    var options = new DeploymentOptions()
//      .setWorker(true)
      .setInstances(size);

    return vertx
      .deployVerticle(() -> {
          var verticle = verticles.get();
          verticle.setTopics(Set.of(config.functionTopicPrefix() + function));
          return verticle;
        },
        options)
      .repeat().atMost(size)
      .invoke(id -> {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("deploy verticle[id={}] for function {} successfully",
            id, function);
        }
        verticleIds.computeIfAbsent(function, key -> new HashSet<>())
          .add(id);
      })
      .collect()
      .last();
  }

  public Uni<Void> deleteVerticle(String function) {
    var ids = verticleIds.get(function);
    if (ids!=null) {
      return Multi.createFrom().iterable(ids)
        .call(id -> vertx.undeploy(id))
        .invoke(id -> LOGGER.info("Undeploy verticle[id={}] for function {} successfully", id, function))
        .collect().last()
        .replaceWithVoid()
        .invoke(() -> verticleIds.remove(function));
    }
    return Uni.createFrom().nullItem();
  }
}
