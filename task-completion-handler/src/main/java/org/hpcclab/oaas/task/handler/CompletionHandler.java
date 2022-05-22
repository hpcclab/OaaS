package org.hpcclab.oaas.task.handler;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.oaas.model.ErrorMessage;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
@RequestScoped
public class CompletionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CompletionHandler.class);

  @Inject
  RoutingContext ctx;
  @Inject
  @RestClient
  CompletionSubmissionService submissionService;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> handle() {
    var headers = ctx.request().headers();
    var ceType = headers.get("ce-type");
    return switch (ceType) {
      case "dev.knative.kafka.event", "oaas.task" -> handleDeadLetter(ctx.getBody())
        .map(ignore -> Response.ok().build());
      case "oaas.task.result" -> handleResult(ctx.getBody())
        .map(ignore -> Response.ok().build());
      default -> Uni.createFrom()
        .item(Response.status(404)
          .entity(new ErrorMessage("Can not handle type: " + ceType)).build());

    };
  }

  public Uni<Void> handleDeadLetter(Buffer body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.info("received dead letter: {}", ceId);
    var error = headers.get("ce-knativeerrordata");
    var taskCompletion = new TaskCompletion()
      .setId(ceId)
      .setSuccess(false)
      .setErrorMsg(error);
    return sendUni(taskCompletion);
  }


  public Uni<Void> handleResult(Buffer body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.info("received task result: {}", ceId);
    return sendUni(tryDecode(ceId,body));
  }

  TaskCompletion tryDecode(String id, Buffer buffer) {
    if (buffer == null) {
      return new TaskCompletion(
        id,
        true,
        null,
        null);
    }
    try {
      return Json.decodeValue(buffer, TaskCompletion.class);
    } catch (DecodeException decodeException){
      return new TaskCompletion(
        id,
        true,
//        decodeException.getMessage(),
        null,
        null);
    }
  }

  Uni<Void> sendUni(TaskCompletion taskCompletion) {
    return submissionService.submit(List.of(taskCompletion));
  }
}
