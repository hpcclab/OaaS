package org.hpcclab.oaas.controller.initializer;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class ServerInitializer {

  @Inject
  BuiltInLoader builtInLoader;

  void onStart(@Observes StartupEvent startupEvent) throws IOException{
    builtInLoader.setup();
  }

}
