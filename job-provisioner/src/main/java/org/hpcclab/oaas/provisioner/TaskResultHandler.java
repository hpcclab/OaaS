package org.hpcclab.oaas.provisioner;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.knative.events.EventAttribute;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;

import javax.ws.rs.core.Context;

public class TaskResultHandler {
  @Channel("task-completions")
  Emitter<Record<String, TaskCompletion>> tasksCompletionEmitter;

  @Funq
  @CloudEventMapping(
    trigger = "oaas.task.result"
  )
  public void handle(@Context CloudEvent<String> cloudEvent) {
//    var url = task.getOutput().getState().getBaseUrl();
//    var completion = new TaskCompletion()
//      .setId(task.getId())
//      .setMainObj(task.getMain().getId())
//      .setOutputObj(task.getOutput().getId())
//      .setFunctionName(task.getFunction().getName())
//      .setStatus(succeeded ? TaskCompletion.Status.SUCCEEDED:TaskCompletion.Status.FAILED)
//      .setStartTime(job.getStatus().getStartTime())
//      .setCompletionTime(job.getStatus().getCompletionTime())
//      .setRequestFile(task.getRequestFile())
//      .setResourceUrl(url)
//      .setDebugCondition(Json.encode(job.getStatus()));
//    var items = client.pods().withLabelSelector(job.getSpec().getSelector())
//      .list().getItems();
//    if (items.size() > 0) {
//      var pod = items.get(0);
//      var log = client.pods().withName(pod.getMetadata().getName())
//        .getLog();
//      completion.setDebugLog(log);
//    }
//    tasksCompletionEmitter.send(
//      Message.of(Record.of(completion.getId(), completion))
//    );
//    LOGGER.debug("{} is submitted", completion);
  }

}
