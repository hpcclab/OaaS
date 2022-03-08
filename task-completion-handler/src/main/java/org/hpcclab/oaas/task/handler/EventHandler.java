package org.hpcclab.oaas.task.handler;

import io.opentelemetry.context.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.TracingMetadata;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.ErrorMessage;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
@RequestScoped

public class EventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);

  @Inject
  RoutingContext ctx;
  @Channel("task-completions")
  MutinyEmitter<TaskCompletion> tasksCompletionEmitter;
  @ConfigProperty(name = "quarkus.opentelemetry.enabled")
  boolean openTelemetryEnabled;
  @Inject
  @RestClient
  CompletionSubmissionService submissionService;

  @POST
  public Uni<Response> handle(String body) {
    var headers = ctx.request().headers();
    var ceType = headers.get("ce-type");
    return switch (ceType) {
      case "dev.knative.kafka.event", "oaas.task" -> handleDeadLetter(body)
        .map(ignore -> Response.ok().build());
      case "oaas.task.result" -> handleResult(body)
        .map(ignore -> Response.ok().build());
      default -> Uni.createFrom()
        .item(Response.status(404)
          .entity(new ErrorMessage("Can not handle type: " + ceType)).build());

    };
  }

  public Uni<Void> handleDeadLetter(String body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.info("received dead letter: {}", ceId);
    var error = headers.get("ce-knativeerrordata");
//    var func = headers.get("ce-function");
    var taskCompletion = new TaskCompletion()
      .setId(ceId)
      .setStatus(TaskStatus.FAILED)
//      .setFunctionName(func)
      .setDebugLog(error);
    return sendUni(taskCompletion);
  }


  public Uni<Void> handleResult(String body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.info("received task result: {}", ceId);
    var objectId = ceId.split("/")[0];
    var succeededHeader = headers.get("ce-tasksucceeded");
    var succeeded = succeededHeader==null || Boolean.parseBoolean(succeededHeader);
    var taskCompletion = new TaskCompletion()
      .setId(objectId)
      .setStatus(succeeded ? TaskStatus.SUCCEEDED:TaskStatus.FAILED)
      .setCompletionTime(System.currentTimeMillis())
      .setDebugLog(body);
    return sendUni(taskCompletion);
  }

  Uni<Void> sendViaKafka(TaskCompletion taskCompletion) {
    if (openTelemetryEnabled) {
      var m = Message.of(taskCompletion, Metadata.of(TracingMetadata.withPrevious(Context.current())));
      tasksCompletionEmitter.send(m);
      return Uni.createFrom().nullItem();
    } else {
      return tasksCompletionEmitter.send(taskCompletion);
    }
  }

  Uni<Void> sendUni(TaskCompletion taskCompletion) {
    return submissionService.submit(List.of(taskCompletion));
  }
}
