package org.hpcclab.oaas.task.handler;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.hpcclab.oaas.iface.service.ObjectService;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Context;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class TaskResultHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskResultHandler.class);
  @Channel("task-completions")
  Emitter<Record<String, TaskCompletion>> tasksCompletionEmitter;

  @Inject
  ObjectService objectService;

  @Funq
  @CloudEventMapping(
    trigger = "oaas.task.result"
  )
  public Uni<Void> handleResult(@Context CloudEvent<Map<String, Object>> cloudEvent, Map<String, Object> body) {
    LOGGER.info("received task result: {}", cloudEvent);
    var id = cloudEvent.id();
    var objectId = id.split("/")[0];
//    var url = task.getOutput().getState().getBaseUrl();
    var succeeded = Boolean.parseBoolean(cloudEvent.extensions().getOrDefault("tasksucceeded", "true"));
    var completion = new TaskCompletion()
      .setId(objectId)
      .setOutputObj(UUID.fromString(objectId))
      .setStatus(succeeded ? TaskCompletion.Status.SUCCEEDED:TaskCompletion.Status.FAILED)
      .setCompletionTime(Instant.now().toString())
      .setDebugLog(Json.encodePrettily(body));


    Uni<Void> uni = null;
    if (cloudEvent.extensions().containsKey("Baseresourceurl")) {
      completion.setResourceUrl(cloudEvent.extensions().get("Baseresourceurl"));
      uni = Uni.createFrom().completionStage(
        tasksCompletionEmitter.send(Record.of(completion.getId(), completion)));
    } else {
      uni = objectService.get(objectId)
        .flatMap(obj -> {
          completion.setMainObj(obj.getOrigin().getParentId())
            .setResourceUrl(obj.getState().getBaseUrl())
            .setFunctionName(obj.getOrigin().getFuncName());
          return Uni.createFrom().completionStage(
            tasksCompletionEmitter.send(Record.of(completion.getId(), completion))
          );
        });
    }
    return uni
      .invoke(() -> LOGGER.debug("{} is submitted", completion));
  }

}
