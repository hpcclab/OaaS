package org.hpcclab.oaas.task.handler;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.TracingMetadata;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;
import java.util.UUID;

@Path("/")
@RequestScoped

public class EventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);

  @Inject
  RoutingContext ctx;
  @Channel("task-completions")
  MutinyEmitter<TaskCompletion> tasksCompletionEmitter;

  @POST
  public void handle(String body) {
    var headers = ctx.request().headers();
    var ceType = headers.get("ce-type");
    switch (ceType) {
      case "dev.knative.kafka.event", "oaas.task" -> handleDeadLetter(body);
      case "oaas.task.result" -> handleResult(body);
      default -> {
        ctx.response().setStatusCode(404)
          .send(new JsonObject().put("msg", "Can not handle type: " + ceType).toString());
      }
    }
  }

  public void handleDeadLetter(String body) {
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
    send(taskCompletion);
  }


  public void handleResult(String body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.info("received task result: {}", ceId);
    var objectId = ceId.split("/")[0];
    var succeededHeader = headers.get("ce-tasksucceeded");
    var succeeded = succeededHeader==null || Boolean.parseBoolean(succeededHeader);
    var taskCompletion = new TaskCompletion()
      .setId(objectId)
      .setOutputObj(UUID.fromString(objectId))
      .setStatus(succeeded ? TaskStatus.SUCCEEDED:TaskStatus.FAILED)
      .setCompletionTime(Instant.now().toString())
      .setDebugLog(body);
    send(taskCompletion);
  }

  void send(TaskCompletion taskCompletion) {
    tasksCompletionEmitter.send(Message.of(taskCompletion, Metadata.of(TracingMetadata.withCurrent(Context.current()))));
  }

  static class HttpServerRequestTextMapGetter implements TextMapGetter<HttpServerRequest> {
    @Override
    public Iterable<String> keys(final HttpServerRequest carrier) {
      return carrier.headers().names();
    }

    @Override
    public String get(final HttpServerRequest carrier, final String key) {
      if (carrier==null) {
        return null;
      }

      return carrier.headers().get(key);
    }
  }
}
