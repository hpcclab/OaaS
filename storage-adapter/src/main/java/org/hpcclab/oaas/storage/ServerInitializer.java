package org.hpcclab.oaas.storage;

import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ServerInitializer {

//  @Inject
//  InfinispanInit infinispanInit;
  @Inject
  ArgRepositoryInitializer initializer;


  void onStart(@Observes StartupEvent startupEvent) {
    initializer.setup();
//    infinispanInit.setup();
  }

}
