package org.hpcclab.oaas.task.handler;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import java.time.Instant;
import java.util.UUID;

public class TaskResultHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskResultHandler.class );
  @Channel("task-completions")
  Emitter<Record<String, TaskCompletion>> tasksCompletionEmitter;

  @Funq
  @CloudEventMapping(
    trigger = "oaas.task.result"
  )
  public void handle(@Context CloudEvent<byte[]> cloudEvent) {
    LOGGER.info("received task result: {}", cloudEvent);
    var id = cloudEvent.id();
    var objectId = id.split("/")[0];
//    var url = task.getOutput().getState().getBaseUrl();
    var succeeded = Boolean.parseBoolean(cloudEvent.extensions().getOrDefault("tasksucceeded", "true"));
    var completion = new TaskCompletion()
      .setId(objectId)
//      .setMainObj(task.getMain().getId())
      .setOutputObj(UUID.fromString(objectId))
//      .setFunctionName(task.getFunction().getName())
      .setStatus(succeeded ? TaskCompletion.Status.SUCCEEDED:TaskCompletion.Status.FAILED)
//      .setStartTime(job.getStatus().getStartTime())
      .setCompletionTime(Instant.now().toString())
//      .setRequestFile(task.getRequestFile())
//      .setResourceUrl(url)
//      .setDebugCondition(Json.encode(job.getStatus()))
        ;
//    var items = client.pods().withLabelSelector(job.getSpec().getSelector())
//      .list().getItems();
//    if (items.size() > 0) {
//      var pod = items.get(0);
//      var log = client.pods().withName(pod.getMetadata().getName())
//        .getLog();
//      completion.setDebugLog(log);
//    }

    tasksCompletionEmitter.send(
      Message.of(Record.of(completion.getId(), completion))
    );
    LOGGER.debug("{} is submitted", completion);
  }

}
