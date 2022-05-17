package org.hpcclab.oaas.provisioner;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
@Deprecated(forRemoval = true)
public class JobWatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(JobWatcher.class);
  @Inject
  KubernetesClient client;


  void startup(@Observes StartupEvent startupEvent) {
    if (!ProfileManager.getLaunchMode().isDevOrTest()) {
      client.batch().v1().jobs().inform(new ResourceEventHandler<>() {
        @Override
        public void onAdd(Job obj) {
          LOGGER.debug("job {} created", obj.getMetadata().getName());

        }

        @Override
        public void onUpdate(Job oldObj, Job newObj) {
          LOGGER.debug("job {} updated", newObj.getMetadata().getName());
          if (!oldObj.getMetadata().getAnnotations()
            .containsKey("oaas.task")) {
            return;
          }
          OaasTask task = Json.decodeValue(oldObj
            .getMetadata().getAnnotations().get("oaas.task"), OaasTask.class);
          if (newObj.getStatus().getSucceeded()!=null &&
            newObj.getStatus().getSucceeded() >= 1) {
            submitTaskCompletion(newObj, task, true);
            client.batch().v1().jobs()
              .delete(newObj);
          } else if (newObj.getStatus().getFailed()!=null &&
            newObj.getStatus().getFailed() >= 1) {
            submitTaskCompletion(newObj, task, false);
            client.batch().v1().jobs()
              .delete(newObj);
          }
        }

        @Override
        public void onDelete(Job obj, boolean deletedFinalStateUnknown) {
          LOGGER.debug("job {} deleted", obj.getMetadata().getName());
        }
      }, 30 * 1000L);
    }
  }

  void submitTaskCompletion(Job job,
                            OaasTask task,
                            boolean succeeded) {
    var completion = new TaskCompletion()
      .setId(task.getId())
//      .setFunctionName(task.getFunction().getName())
      .setSuccess(succeeded)
//      .setStartTime(Instant.parse(job.getStatus().getStartTime()).toEpochMilli())
//      .setCompletionTime(Instant.parse(job.getStatus().getCompletionTime()).toEpochMilli());
      ;
    var items = client.pods().withLabelSelector(job.getSpec().getSelector())
      .list().getItems();
    if (!items.isEmpty()) {
      var pod = items.get(0);
      var log = client.pods().withName(pod.getMetadata().getName())
        .getLog();
      completion.setDebugLog(log);
    }
//    tasksCompletionEmitter.send(
//      Message.of(Record.of(completion.getId(), completion))
//    );
//    LOGGER.debug("{} is submitted", completion);
  }
}
