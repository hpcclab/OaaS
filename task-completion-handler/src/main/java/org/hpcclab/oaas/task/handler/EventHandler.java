package org.hpcclab.oaas.task.handler;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.hpcclab.oaas.iface.service.ObjectService;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Path("/")
@RequestScoped

public class EventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger( EventHandler.class );

  @Inject
  RoutingContext ctx;
  @Channel("task-completions")
  Emitter<Record<String, TaskCompletion>> tasksCompletionEmitter;
  @Inject
  ObjectService objectService;

  @POST
  public Uni<Void> handle(String body) {
    var headers = ctx.request().headers();
    LOGGER.debug("Handle headers: {}", headers);
    var ceType = headers.get("ce-type");
    return switch (ceType){
      case "dev.knative.kafka.event" -> handleDeadLetter(body);
      case "oaas.task.result" -> handleResult(body);
      default -> {
        ctx.response().setStatusCode(404)
          .send(new JsonObject().put("msg", "Can not handle type: " +ceType).toString());
        yield Uni.createFrom().nullItem();
      }
    };
  }

  public Uni<Void> handleDeadLetter(String body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.info("received dead letter: {}", ceId);
    var error = headers.get("ce-knativeerrordata");
    var task = Json.decodeValue(body, OaasTask.class);
    var taskCompletion = new TaskCompletion()
      .setId(task.getId())
      .setStatus(TaskStatus.FAILED)
      .setOutputObj(task.getOutput().getId())
      .setMainObj(task.getMain().getId())
      .setFunctionName(task.getFunction().getName())
      .setDebugLog(error);
    tasksCompletionEmitter.send(Record.of(taskCompletion.getId(), taskCompletion));
    return Uni.createFrom().nullItem();
  }


  public Uni<Void> handleResult(String body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.info("received task result: {}", ceId);
    var objectId = ceId.split("/")[0];
    var succeeded = Boolean.parseBoolean(headers.get("ce-tasksucceeded"));
    var completion = new TaskCompletion()
      .setId(objectId)
      .setOutputObj(UUID.fromString(objectId))
      .setStatus(succeeded ? TaskStatus.SUCCEEDED:TaskStatus.FAILED)
      .setCompletionTime(Instant.now().toString())
      .setDebugLog(body);


    Uni<Void> uni = null;
    if (headers.contains("ce-Baseresourceurl")) {
      completion.setResourceUrl(headers.get("ce-Baseresourceurl"));
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
