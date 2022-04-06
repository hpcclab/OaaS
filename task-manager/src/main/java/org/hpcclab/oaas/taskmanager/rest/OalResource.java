package org.hpcclab.oaas.taskmanager.rest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.repository.TaskCompletionRepository;
import org.hpcclab.oaas.repository.function.handler.FunctionRouter;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;
import org.hpcclab.oaas.taskmanager.service.ContentUrlGenerator;
import org.hpcclab.oaas.taskmanager.service.ObjectCompletionListener;
import org.hpcclab.oaas.taskmanager.service.TaskCompletionListener;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

@Path("/oal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OalResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(OalResource.class);
  @Inject
  FunctionRouter router;
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  TaskEventManager taskEventManager;
  @Inject
  TaskCompletionRepository completionRepo;
  @Inject
//  TaskCompletionListener completionListener;
  ObjectCompletionListener completionListener;
  @Inject
  ContentUrlGenerator contentUrlGenerator;
  @Inject
  TaskManagerConfig config;

  @POST
  public Uni<OaasObject> getObjectWithPost(ObjectAccessLangauge oal) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      return execFunction(oal)
        .call(obj -> taskEventManager.submitCreateEvent(obj.getId().toString()));
    } else {
      return objectRepo.getAsync(oal.getTarget())
        .onItem().ifNull()
        .failWith(() -> NoStackException.notFoundObject(oal.getTarget(), 404));
    }
  }

  @GET
  @Path("{oal}")
  public Uni<OaasObject> getObject(@PathParam("oal") String oal) {
    var oaeObj = ObjectAccessLangauge.parse(oal);
    LOGGER.debug("Receive OAE getObject '{}'", oaeObj);
    return getObjectWithPost(oaeObj);
  }

  @POST
  @Path("-/{filePath:.*}")
  public Uni<Response> postContentAndExec(@PathParam("filePath") String filePath,
                                         @QueryParam("await") Boolean await,
                                         ObjectAccessLangauge oal) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      return execFunction(oal)
        .flatMap(obj -> submitAndWaitObj(obj.getId(), await)
          .map(newObj -> createResponse(newObj, filePath))
        );

    } else {
      return objectRepo.getAsync(oal.getTarget())
        .flatMap(obj -> {
          if (obj.getOrigin().getParentId() == null) {
            return Uni.createFrom().item(createResponse(obj, filePath));
          } else if (obj.getTask() == null) {
            return submitAndWaitObj(obj.getId(), await)
              .map(newObj -> createResponse(newObj, filePath));
          } else {
            return Uni.createFrom().item(createResponse(obj, filePath));
          }
        });
    }
  }


  @GET
  @Path("{oal}/{filePath:.*}")
  public Uni<Response> getContentAndExec(@PathParam("oal") String oal,
                                         @PathParam("filePath") String filePath,
                                         @QueryParam("await") Boolean await) {
    var oaeObj = ObjectAccessLangauge.parse(oal);
    LOGGER.debug("Receive OAL getContent '{}' '{}'", oaeObj, filePath);
    return postContentAndExec(filePath, await, oaeObj);
  }

  public Uni<OaasObject> execFunction(ObjectAccessLangauge oal) {
    var uni = router.functionCall(oal)
      .map(FunctionExecContext::getOutput);
    if (LOGGER.isDebugEnabled()) {
      uni = uni
        .invoke(() -> LOGGER.debug("Call function '{}' succeed", oal));
    }
    return uni;
  }

  public Response createResponse(OaasObject object,
                                 String filePath) {
    if (object==null) return Response.status(404).build();
    if (object.getOrigin().getParentId()!=null) {
      var taskCompletion = object.getTask();
      if (taskCompletion==null) {
        return Response.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build();
      }
      if (taskCompletion.getStatus()==TaskStatus.DOING) {
        return Response.status(HttpResponseStatus.NO_CONTENT.code())
          .build();
      }
      if (taskCompletion.getStatus()!=TaskStatus.SUCCEEDED) {
        return Response.status(HttpResponseStatus.FAILED_DEPENDENCY.code()).build();
      }
    }
    var oUrl = object.getState().getOverrideUrls();
    if (oUrl!= null && oUrl.containsKey(filePath))
      return Response.status(HttpResponseStatus.FOUND.code())
        .location(URI.create(oUrl.get(filePath)))
        .build();
    var fileUrl = contentUrlGenerator.generateUrl(object, filePath);
    return Response.status(HttpResponseStatus.FOUND.code())
      .location(URI.create(fileUrl))
      .build();
  }

//  public Uni<TaskCompletion> submitAndWait(String id,
//                                           Boolean await) {
//    var uni1 = taskEventManager.submitCreateEvent(id)
//      .onFailure().invoke(e -> LOGGER.error("Got an error when submitting CreateEvent", e));
//    if (await==null ? config.defaultBlockCompletion():await) {
//      var uni2 = completionListener.wait(id);
//      return Uni.combine().all().unis(uni1, uni2)
//        .asTuple()
//        .flatMap(event -> completionRepo.getAsync(id));
//    }
//    return uni1
//      .flatMap(event -> completionRepo.getAsync(id))
//      .replaceIfNullWith(() -> new TaskCompletion().setStatus(TaskStatus.DOING));
//  }

  public Uni<OaasObject> submitAndWaitObj(String id, Boolean await) {
    var uni1 = taskEventManager.submitCreateEvent(id)
      .onFailure().invoke(e -> LOGGER.error("Got an error when submitting CreateEvent", e));
    if (await==null ? config.defaultBlockCompletion():await) {
      var uni2 = completionListener.wait(id);
      return Uni.combine().all().unis(uni1, uni2)
        .asTuple()
        .flatMap(event -> objectRepo.getAsync(id));
    }
    return uni1
      .flatMap(event -> objectRepo.getAsync(id));
  }
}
