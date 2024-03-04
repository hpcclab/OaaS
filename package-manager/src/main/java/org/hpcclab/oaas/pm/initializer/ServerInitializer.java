package org.hpcclab.oaas.pm.initializer;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class ServerInitializer {

  final BuiltInLoader builtInLoader;

  @Inject
  public ServerInitializer(BuiltInLoader builtInLoader) {
    this.builtInLoader = builtInLoader;
  }

  void onStart(@Observes StartupEvent startupEvent) throws IOException{
    builtInLoader.setup();
  }

}
