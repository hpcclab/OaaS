package org.hpcclab.msc.provisioner;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.hpcclab.msc.object.model.Task;
import org.hpcclab.msc.object.model.TaskCompletion;
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
  @Channel("task-completions")
  Emitter<Record<String,TaskCompletion>> tasksCompletionEmitter;
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
              submitTaskCompletion(resource,task, true);
              client.batch().v1().jobs()
                .delete(resource);
            }
            else if (resource.getStatus().getFailed()!= null &&
              resource.getStatus().getFailed() >= 1) {
              submitTaskCompletion(resource,task, false);
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

  void submitTaskCompletion(Job job,
                            Task task,
                            boolean succeeded) {
    var url = task.getEnv().get("OUTPUT_RESOURCE_URL");
    if (task.getResourceType().endsWith("FILES")) {
      url = url + "/" + task.getEnv().get("REQUEST_FILE");
    }

    var completion = new TaskCompletion()
      .setMainObj(task.getMainObj())
      .setOutputObj(task.getOutputObj())
      .setFunctionName(task.getFunctionName())
      .setStatus(succeeded? TaskCompletion.Status.SUCCEEDED: TaskCompletion.Status.FAILED)
      .setStartTime(job.getStatus().getStartTime())
      .setCompletionTime(job.getStatus().getCompletionTime())
      .setRequestFile(task.getEnv().get("REQUEST_FILE"))
      .setDebugMessage(Json.encode(job.getStatus()))
      .setResourceUrl(url);
    tasksCompletionEmitter.send(
      Message.of(Record.of(completion.getOutputObj() + "/" + completion.getRequestFile(),completion))
    );
    LOGGER.info("{} is submitted", completion);
  }

  void onShutdown(@Observes ShutdownEvent shutdownEvent) {
    if (watch != null) {
      watch.close();
    }
  }
}
