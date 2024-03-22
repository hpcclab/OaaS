package org.hpcclab.oaas.crm;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.crm.observe.CrMetricObserver;
import org.hpcclab.oaas.crm.observe.ObserverLoopRunner;
import org.hpcclab.oaas.crm.template.CrTemplateManager;

@Singleton
public class CrmInitializer {
  final CrControllerManager controllerManager;
  final CrMetricObserver observer;
  final ObserverLoopRunner runner;
  final CrTemplateManager templateManager;
  final Vertx vertx;
  final CrmConfig config;

  @Inject
  public CrmInitializer(CrControllerManager controllerManager,
                        CrMetricObserver observer,
                        ObserverLoopRunner runner,
                        CrTemplateManager templateManager, Vertx vertx, CrmConfig config) {
    this.controllerManager = controllerManager;
    this.observer = observer;
    this.runner = runner;
    this.templateManager = templateManager;
    this.vertx = vertx;
    this.config = config;
  }

  public void onStart(@Observes StartupEvent event) {
    if (config.loadTemplateOnStart()) {
      vertx.executeBlockingAndAwait(() -> {
          templateManager.loadTemplate();
          templateManager.initTemplates(controllerManager);
          controllerManager.loadAllToLocal();
          runner.setup();
          return 0;
        }
      );
    }
  }
}
