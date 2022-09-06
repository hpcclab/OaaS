package org.hpcclab.oaas.storage;

import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.infinispan.InfinispanInit;

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
