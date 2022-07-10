package org.hpcclab.oaas.taskmanager.rest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.function.FunctionRouter;
import org.hpcclab.oaas.repository.function.InvocationGraphExecutor;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;
import org.hpcclab.oaas.taskmanager.service.ContentUrlGenerator;
import org.hpcclab.oaas.taskmanager.service.ObjectCompletionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/oal/")
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
  InvocationGraphExecutor graphExecutor;
  @Inject
  ObjectCompletionListener completionListener;
  @Inject
  ContentUrlGenerator contentUrlGenerator;
  @Inject
  TaskManagerConfig config;

  @POST
  public Uni<OaasObject> getObjectWithPost(ObjectAccessLangauge oal,
                                           @QueryParam("await") Boolean await,
                                           @QueryParam("timeout") Integer timeout) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      return execFunction(oal)
        .flatMap(ctx -> submitAndWaitObj(ctx, await!=null && await, timeout)
          .invoke(o -> {
            // NOTE Temporary fix for object status data lost problem.
            var status = o.getStatus();
            if (status.getTaskStatus().isCompleted()) {
              if (status.getSubmittedTime() <= 0)
                status.setSubmittedTime(ctx.getOutput().getStatus().getSubmittedTime());
              if (status.getCompletedTime() <= 0)
                status.setCompletedTime(System.currentTimeMillis());
            }
          }));
    } else {
      return objectRepo.getAsync(oal.getTarget())
        .onItem().ifNull()
        .failWith(() -> NoStackException.notFoundObject(oal.getTarget(), 404));
    }
  }

  @GET
  @Path("{oal}")
  public Uni<OaasObject> getObject(@PathParam("oal") String oal,
                                   @QueryParam("await") Boolean await,
                                   @QueryParam("timeout") Integer timeout) {
    var oaeObj = ObjectAccessLangauge.parse(oal);
    LOGGER.debug("Receive OAE getObject '{}'", oaeObj);
    return getObjectWithPost(oaeObj, await, timeout);
  }

  @POST
  @Path("-/{filePath:.*}")
  public Uni<Response> postContentAndExec(@PathParam("filePath") String filePath,
                                          @QueryParam("await") Boolean await,
                                          @QueryParam("timeout") Integer timeout,
                                          ObjectAccessLangauge oal) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      return execFunction(oal)
        .flatMap(ctx -> submitAndWaitObj(ctx, await, timeout)
          .map(newObj -> createResponse(newObj, filePath))
        );

    } else {
      return objectRepo.getAsync(oal.getTarget())
        .onItem().ifNull().failWith(() -> NoStackException.notFoundObject(oal.getTarget(), 404))
        .flatMap(obj -> {
          if (obj.isReadyToUsed()) {
            return Uni.createFrom().item(createResponse(obj, filePath));
          }
          if (obj.getStatus().getTaskStatus().isFailed()) {
            return Uni.createFrom().item(createResponse(obj, filePath));
          }
          if (!obj.getStatus().getTaskStatus().isSubmitted()) {
            return waitObj(obj, timeout)
              .map(newObj -> createResponse(newObj, filePath));
          }
          return Uni.createFrom().item(createResponse(obj, filePath));
        });
    }
  }


  @GET
  @Path("{oal}/{filePath:.*}")
  public Uni<Response> getContentAndExec(@PathParam("oal") String oal,
                                         @PathParam("filePath") String filePath,
                                         @QueryParam("await") Boolean await,
                                         @QueryParam("timeout") Integer timeout) {
    var oaeObj = ObjectAccessLangauge.parse(oal);
    LOGGER.debug("Receive OAL getContent '{}' '{}'", oaeObj, filePath);
    return postContentAndExec(filePath, await, timeout, oaeObj);
  }

  public Uni<FunctionExecContext> execFunction(ObjectAccessLangauge oal) {
    var uni = router.invoke(oal)
      .flatMap(objectRepo::persistFromCtx);
    if (LOGGER.isDebugEnabled()) {
      uni = uni
        .invoke(() -> LOGGER.debug("Call function '{}' succeed", oal));
    }
    return uni;
  }

  public Response createResponse(OaasObject object,
                                 String filePath) {
    return createResponse(object, filePath, HttpResponseStatus.SEE_OTHER.code());
  }

  public Response createResponse(OaasObject object,
                                 String filePath,
                                 int redirectCode) {
    if (object==null) return Response.status(404).build();
    if (object.getOrigin().getParentId()!=null) {
      var status = object.getStatus();
      if (status==null) {
        return Response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).build();
      }
      var ts = status.getTaskStatus();
      if (ts==TaskStatus.DOING) {
        return Response.status(HttpResponseStatus.GATEWAY_TIMEOUT.code())
          .build();
      }
      if (ts.isFailed()) {
        return Response.status(HttpResponseStatus.FAILED_DEPENDENCY.code()).build();
      }
    }
    var oUrl = object.getState().getOverrideUrls();
    if (oUrl!=null && oUrl.containsKey(filePath))
      return Response.status(redirectCode)
        .location(URI.create(oUrl.get(filePath)))
        .build();
    var fileUrl = contentUrlGenerator.generateUrl(object, filePath);
    return Response.status(redirectCode)
      .location(URI.create(fileUrl))
      .build();
  }


  public Uni<OaasObject> submitAndWaitObj(FunctionExecContext ctx,
                                          Boolean await,
                                          Integer timeout) {
    if (await==null ? config.defaultAwaitCompletion():await) {
      var id = ctx.getOutput().getId();
      var uni1 = completionListener.wait(id, timeout);
      var uni2 = graphExecutor.exec(ctx);
      return Uni.combine().all().unis(uni1, uni2)
        .asTuple()
        .flatMap(tuple -> objectRepo.getAsync(id)
//          .invoke(Unchecked.consumer(obj -> {
//            if (!obj.getStatus().getTaskStatus().isCompleted())
//              throw new IllegalStateException();
//          }))
//          .onFailure(IllegalStateException.class)
//          .retry().withBackOff(Duration.ofMillis(200)).atMost(2)
//          .onFailure(IllegalStateException.class)
//          .recoverWithUni(objectRepo.getAsync(id))
        );
    }
    return graphExecutor.exec(ctx)
      .replaceWith(ctx.getOutput());
  }

  public Uni<OaasObject> waitObj(OaasObject obj,
                                 Integer timeout) {
    var status = obj.getStatus();
    var ts = status.getTaskStatus();
    if (!ts.isSubmitted() && !status.isInitWaitFor()) {
      var uni1 = completionListener.wait(obj.getId(), timeout);
      var uni2 = graphExecutor.exec(obj);
      return Uni.combine().all().unis(uni1, uni2)
        .asTuple()
        .flatMap(v -> objectRepo.getAsync(obj.getId()));

    }
    return completionListener.wait(obj.getId(), timeout)
      .flatMap(event -> objectRepo.getAsync(obj.getId()));
  }
}
