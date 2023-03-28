package org.hpcclab.oaas.controller.initializer;

import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class ServerInitializer {

  @Inject
  BuiltInLoader builtInLoader;
  @Inject
  ArgRepositoryInitializer initializer;


  void onStart(@Observes StartupEvent startupEvent) throws IOException, ExecutionException, InterruptedException {
//    infinispanInit.setup();
    initializer.setup();
    builtInLoader.setup();
  }

}
