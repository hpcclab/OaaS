package org.hpcclab.oaas.sa;

import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ServerInitializer {
  @Inject
  ArgRepositoryInitializer initializer;


  void onStart(@Observes StartupEvent startupEvent) {
    initializer.setup();
  }

}
