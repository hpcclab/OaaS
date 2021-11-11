package org.hpcclab.oaas.task.handler;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadLetterHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger( DeadLetterHandler.class );
  @Channel("task-completions")
  Emitter<Record<String, TaskCompletion>> tasksCompletionEmitter;
  @Funq
  @CloudEventMapping(
    trigger = "dev.knative.kafka.event"
  )
  public void handleDeadLetter(OaasTask task, @Context CloudEvent<OaasTask> cloudEvent) {
    LOGGER.info("received dead letter: {}", cloudEvent);
    var taskCompletion = new TaskCompletion()
      .setId(task.getId())
      .setStatus(TaskCompletion.Status.FAILED)
      .setOutputObj(task.getOutput().getId())
      .setMainObj(task.getMain().getId())
      .setFunctionName(task.getFunction().getName())
      .setDebugLog(cloudEvent.extensions().get("Knativeerrordata"));
    tasksCompletionEmitter.send(Record.of(taskCompletion.getId(), taskCompletion));
  }
}
