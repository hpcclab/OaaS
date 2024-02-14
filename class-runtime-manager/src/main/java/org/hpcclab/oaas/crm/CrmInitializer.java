package org.hpcclab.oaas.crm;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.crm.observe.CrMetricObserver;
import org.hpcclab.oaas.crm.observe.ObserverLoopRunner;

@Singleton
public class CrmInitializer {
  final CrControllerManager controllerManager;
  final CrMetricObserver observer;

  final ObserverLoopRunner runner;

  @Inject
  public CrmInitializer(CrControllerManager controllerManager, CrMetricObserver observer, ObserverLoopRunner runner) {
    this.controllerManager = controllerManager;
    this.observer = observer;
    this.runner = runner;
  }

  public void onStart(@Observes StartupEvent event) {
    controllerManager.loadAllToLocal();
    runner.setup();
  }
}
