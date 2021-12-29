package org.hpcclab.oaas.taskmanager.initializer;

import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class ServerInitializer {

  @Inject
  InfinispanInit infinispanInit;


  void onStart(@Observes StartupEvent startupEvent) {
    infinispanInit.setup();
  }

}
