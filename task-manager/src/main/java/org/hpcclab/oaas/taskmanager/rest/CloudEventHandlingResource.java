package org.hpcclab.oaas.taskmanager.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.hpcclab.oaas.invocation.TaskDecoder;
import org.hpcclab.oaas.model.ErrorMessage;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/ce")
@RequestScoped
public class CloudEventHandlingResource {
  private static final Logger LOGGER = LoggerFactory.getLogger( CloudEventHandlingResource.class );

  @Inject
  RoutingContext ctx;
  @Inject
  InvocationExecutor graphExecutor;
  @Inject
  ObjectCompletionPublisher completionPublisher;

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
      .setId(TaskIdentity.decode(ceId))
      .setSuccess(false)
      .setErrorMsg(error);
    return graphExecutor.complete(taskCompletion);
  }


  public Uni<Void> handleResult(Buffer body) {
    var headers = ctx.request().headers();
    var ceId = headers.get("ce-id");
    LOGGER.debug("received task result: {}", ceId);
    var tc = TaskDecoder.tryDecode(ceId, body);
    var id = tc.getId();
    return graphExecutor.complete(tc)
      .invoke(() -> completionPublisher.publish(id.oId() == null?
        id.mId():tc.getId().oId()));
  }
}
