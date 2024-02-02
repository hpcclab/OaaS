package org.hpcclab.oaas.crm;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.hpcclab.oaas.crm.controller.FnEventManager;

@ApplicationScoped
public class CrmInitializer {

  FnEventManager fnEventManager;

  @Inject
  public CrmInitializer(FnEventManager fnEventManager) {
    this.fnEventManager = fnEventManager;
  }

  public void setup(@Observes StartupEvent event) {
    fnEventManager.start();
  }
}
