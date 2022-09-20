package org.hpcclab.oaas.storage;

import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

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
