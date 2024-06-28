package org.hpcclab.oaas.pm.initializer;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.BuiltInLoader;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;

@ApplicationScoped
public class PmInitializer {

  final BuiltInLoader builtInLoader;

  @Inject
  public PmInitializer(ClassRepository clsRepo, FunctionRepository fnRepo) {
    this.builtInLoader = new BuiltInLoader(clsRepo, fnRepo);
  }

  void onStart(@Observes StartupEvent startupEvent) throws Throwable {
    VertxContextSupport.subscribeAndAwait(() ->
      Vertx.currentContext().executeBlocking(() -> {
        builtInLoader.setup();
        return null;
      }));
  }

}
