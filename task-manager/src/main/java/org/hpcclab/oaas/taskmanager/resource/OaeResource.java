package org.hpcclab.oaas.taskmanager.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.ObjectAccessExpression;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.repository.function.handler.FunctionRouter;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/oae")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OaeResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(OaeResource.class);
  @Inject
  FunctionRouter router;
  @Inject
  BlockingContentResource blockingContentResource;
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  TaskEventManager taskEventManager;

  @POST
  public Uni<OaasObject> getObjectWithPost(ObjectAccessExpression oaeObj) {
    if (oaeObj==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oaeObj.getFunctionName()!=null) {
      return execFunction(oaeObj)
        .call(obj -> taskEventManager.submitCreateEvent(obj.getId().toString()));
    } else {
      return objectRepo.getAsync(oaeObj.getTarget())
        .onItem().ifNull()
        .failWith(() -> NoStackException.notFoundObject(oaeObj.getTarget(), 404));
    }
  }

  @GET
  @Path("{oae}")
  public Uni<OaasObject> getObject(@PathParam("oae") String oae) {
    var oaeObj = ObjectAccessExpression.parse(oae);
    LOGGER.debug("Receive OAE getObject '{}'", oaeObj);
    return getObjectWithPost(oaeObj);
  }

  @POST
  @Path("-/{filePath:.*}")
  public Uni<Response> getContentWithPost(@PathParam("filePath") String filePath,
                                          ObjectAccessExpression oaeObj) {
    if (oaeObj==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oaeObj.getFunctionName()!=null) {
      return execFunction(oaeObj)
        .flatMap(obj -> blockingContentResource.submitAndWait(obj.getId())
          .map(taskCompletion -> createResponse(obj, filePath, taskCompletion))
        );

    } else {
      return objectRepo.getAsync(oaeObj.getTarget())
        .flatMap(obj -> {
          if (obj.getOrigin().getParentId()==null) {
            return Uni.createFrom().item(blockingContentResource.createResponse(obj, filePath));
          } else {
            return blockingContentResource.getCompletion(obj.getId())
              .onItem().ifNull()
              .switchTo(() -> blockingContentResource.submitAndWait(obj.getId()))
              .map(taskCompletion -> createResponse(
                obj, filePath, taskCompletion));
          }
        });
    }
  }

  @GET
  @Path("{oae}/{filePath:.*}")
  public Uni<Response> getContentWithPost(@PathParam("oae") String oae,
                                          @PathParam("filePath") String filePath) {
    var oaeObj = ObjectAccessExpression.parse(oae);
    LOGGER.debug("Receive OAE getContent '{}' '{}'", oaeObj, filePath);
    return getContentWithPost(filePath, oaeObj);
  }

  public Uni<OaasObject> execFunction(ObjectAccessExpression oae) {
    return router.functionCall(oae)
      .invoke(() -> LOGGER.debug("Call function '{}' succeed", oae))
      .map(FunctionExecContext::getOutput);
  }

  public Response createResponse(OaasObject object,
                                 String filePath,
                                 TaskCompletion taskCompletion) {
    if (taskCompletion==null) {
      return Response.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build();
    }
    if (taskCompletion.getStatus()!=TaskStatus.SUCCEEDED) {
      return Response.status(HttpResponseStatus.FAILED_DEPENDENCY.code()).build();
    }
    return blockingContentResource.createResponse(object, filePath);
  }
}
