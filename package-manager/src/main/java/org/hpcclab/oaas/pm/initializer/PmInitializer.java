package org.hpcclab.oaas.pm.initializer;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PmInitializer {

  final BuiltInLoader builtInLoader;

  @Inject
  public PmInitializer(BuiltInLoader builtInLoader) {
    this.builtInLoader = builtInLoader;
  }

  void onStart(@Observes StartupEvent startupEvent) throws Throwable {
    VertxContextSupport.subscribeAndAwait(() ->
      Vertx.currentContext().executeBlocking(() -> {
        builtInLoader.setup();
        return null;
      }));
  }

}
