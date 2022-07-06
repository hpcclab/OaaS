package org.hpcclab.oaas.taskmanager.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.hpcclab.oaas.model.ErrorMessage;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.function.InvocationGraphExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/ce")
@RequestScoped
public class CloudEventHandlingResource {
  private static final Logger LOGGER = LoggerFactory.getLogger( CloudEventHandlingResource.class );

  @Inject
  RoutingContext ctx;
  @Inject
  InvocationGraphExecutor graphExecutor;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> handle(byte[] body) {
    var headers = ctx.request().headers();
    var ceType = headers.get("ce-type");
    return switch (ceType) {
      case "dev.knative.kafka.event", "oaas.task" -> handleDeadLetter(Buffer.buffer(body))
        .map(ignore -> Response.ok().build());
      case "oaas.task.result" -> handleResult(Buffer.buffer(body))
        .map(ignore -> Response.ok().build());
      default -> Uni.createFrom()
        .item(Response.status(400)
          .entity(new ErrorMessage("Can not handle type: " + ceType)).build());
    };
  }

  public Uni<Void> handleDeadLetter(Buffer body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.debug("received dead letter: {}", ceId);
    var error = headers.get("ce-knativeerrordata");
    var taskCompletion = new TaskCompletion()
      .setId(ceId)
      .setSuccess(false)
      .setErrorMsg(error);
    return graphExecutor.complete(taskCompletion);
  }


  public Uni<Void> handleResult(Buffer body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.debug("received task result: {}", ceId);
    return graphExecutor.complete(tryDecode(ceId, body));
  }

  TaskCompletion tryDecode(String id, Buffer buffer) {
    var ts = System.currentTimeMillis();
    if (buffer==null) {
      return new TaskCompletion(id, false,
        "Can not parse the task completion message because buffer is null",
        null, null, ts);
    }
    try {
      var completion = Json.decodeValue(buffer, TaskCompletion.class);
      if (completion != null){
        return completion
          .setTs(ts);
      }

    } catch (DecodeException decodeException) {
      LOGGER.warn("Decode failed on id {} : {}", id, decodeException.getMessage());
      return new TaskCompletion(
        id,
        true,
        decodeException.getMessage(),
//        null,
        null,
        null,
        ts);
    }

    return new TaskCompletion(id, false,
      "Can not parse the task completion message", null, null, ts);
  }
}
