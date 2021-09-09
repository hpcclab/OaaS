package org.hpcclab.msc;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import io.vertx.core.json.Json;
import org.hpcclab.msc.object.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class JobWatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger( JobWatcher.class );
  @Inject
  KubernetesClient client;
  private Watch watch;

  void startup(@Observes StartupEvent startupEvent) {
    if (!ProfileManager.getLaunchMode().isDevOrTest()) {
      watch = client.batch().v1().jobs()
        .watch(new Watcher<Job>() {
          @Override
          public void eventReceived(Action action, Job resource) {
            if (resource.getMetadata().getAnnotations() == null ||
              !resource.getMetadata().getAnnotations().containsKey("oaas.task")) {
              return;
            }
            if (action == Action.DELETED || action == Action.ADDED) return;
            Task task = Json.decodeValue(resource.getMetadata().getAnnotations().get("oaas.task"), Task.class);
            if (resource.getStatus().getSucceeded() != null &&
                resource.getStatus().getSucceeded() >= 1) {
              LOGGER.info("object[id={}, parent={}, func={}] is succeeded",
                task.getOutputObj(),
                task.getMainObj(),
                task.getFunctionName()
              );
              client.batch().v1().jobs()
                .delete(resource);
            }
            else if (resource.getStatus().getFailed()!= null &&
              resource.getStatus().getFailed() >= 1) {
              LOGGER.info("object[id={}, parent={}, func={}] is failed. {}",
                task.getOutputObj(),
                task.getMainObj(),
                task.getFunctionName(),
                resource.getStatus()
              );
              client.batch().v1().jobs()
                .delete(resource);
            }
          }

          @Override
          public void onClose(WatcherException cause) {
            LOGGER.error("Job watcher unexpectedly close", cause);
          }
        });
    }
  }


  void onShutdown(@Observes ShutdownEvent shutdownEvent) {
    if (watch != null) {
      watch.close();
    }
  }
}
