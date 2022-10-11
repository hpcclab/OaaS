package org.hpcclab.oaas.invoker.verticle;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class VerticleDeployer {

  @Inject
  ArgRepositoryInitializer initializer;

  void init(@Observes StartupEvent ev,
            Vertx vertx,
            Instance<TaskInvocationVerticle> verticles) {
    initializer.setup();
    int size = 0;
    for (var __ : vertx.nettyEventLoopGroup()) {
      size++;
    }

    vertx
      .deployVerticle(verticles::get, new DeploymentOptions().setInstances(size))
      .await().indefinitely();
  }
}
