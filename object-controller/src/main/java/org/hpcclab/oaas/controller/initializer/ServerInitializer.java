package org.hpcclab.oaas.controller.initializer;

import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.infinispan.InfinispanInit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class ServerInitializer {

  @Inject
  BuiltInLoader builtInLoader;
  @Inject
  InfinispanInit infinispanInit;


  void onStart(@Observes StartupEvent startupEvent) throws IOException, ExecutionException, InterruptedException {
    infinispanInit.setup();
    builtInLoader.setup();
  }

}
