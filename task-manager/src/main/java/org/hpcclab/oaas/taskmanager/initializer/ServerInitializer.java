package org.hpcclab.oaas.taskmanager.initializer;

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
//    infinispanInit.setup();
    initializer.setup();
  }

}
