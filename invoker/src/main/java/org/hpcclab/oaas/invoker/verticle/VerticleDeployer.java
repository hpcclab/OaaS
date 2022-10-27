package org.hpcclab.oaas.invoker.verticle;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VerticleDeployer {
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

  private ConcurrentHashMap<String, String> verticleId = new ConcurrentHashMap<>();

  void init(@Observes StartupEvent ev) {
    initializer.setup();
    var funcPage = funcRepo.pagination(0, 1000);
    var funcList = funcPage.getItems();
    Multi.createFrom().iterable(funcList)
      .onItem().transformToUniAndConcatenate(func -> deployNewConsumer(func.getName()))
      .collect().asList()
      .await().indefinitely();
  }

  public Uni<String> deployNewConsumer(String function) {
    int size = config.numOfVerticle();
    var options = new DeploymentOptions()
//      .setWorker(true)
      .setInstances(size);
    var uni = vertx
      .deployVerticle(() -> {
        var verticle = verticles.get();
        verticle.setTopics(Set.of(config.functionTopicPrefix() + function));
        return verticle;
      }, options)
      .invoke(id -> verticleId.put(function, id));
    if (LOGGER.isInfoEnabled()) {
      uni = uni.invoke(id -> {
        LOGGER.info("deploy verticle[id={}] for function {} successfully",
          id, function);
      });
    }
    return uni;
  }


}
