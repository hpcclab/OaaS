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
//  TaskEventManager taskEventManager;
  InvocationGraphExecutor graphExecutor;
  @Inject
  ObjectCompletionListener completionListener;
  @Inject
  ContentUrlGenerator contentUrlGenerator;
  @Inject
  TaskManagerConfig config;

  @POST
  public Uni<OaasObject> getObjectWithPost(ObjectAccessLangauge oal,
                                           @QueryParam("await") Boolean await) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      var uni = execFunction(oal)
        .call(ctx -> graphExecutor.exec(ctx))
        .map(FunctionExecContext::getOutput);
      if (await != null && await) {
        uni = uni.flatMap(this::waitObj);
      }
      return uni;
    } else {
      return objectRepo.getAsync(oal.getTarget())
        .onItem().ifNull()
        .failWith(() -> NoStackException.notFoundObject(oal.getTarget(), 404));
    }
  }

  @GET
  @Path("{oal}")
  public Uni<OaasObject> getObject(@PathParam("oal") String oal,
                                   @QueryParam("await") Boolean await) {
    var oaeObj = ObjectAccessLangauge.parse(oal);
    LOGGER.debug("Receive OAE getObject '{}'", oaeObj);
    return getObjectWithPost(oaeObj, await);
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
        .flatMap(ctx -> submitAndWaitObj(ctx, await)
          .map(newObj -> createResponse(newObj, filePath))
        );

    } else {
      return objectRepo.getAsync(oal.getTarget())
        .flatMap(obj -> {
          if (obj.isReadyToUsed()) {
            return Uni.createFrom().item(createResponse(obj, filePath));
          }
          if (obj.getStatus().getTaskStatus().isFailed()) {
            return Uni.createFrom().item(createResponse(obj, filePath));
          }
          if (!obj.getStatus().getTaskStatus().isSubmitted()) {
            return waitObj(obj)
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
                                         @QueryParam("await") Boolean await) {
    var oaeObj = ObjectAccessLangauge.parse(oal);
    LOGGER.debug("Receive OAL getContent '{}' '{}'", oaeObj, filePath);
    return postContentAndExec(filePath, await, oaeObj);
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

  public Uni<OaasObject> submitAndWaitObj(OaasObject obj, Boolean await) {
    var uni1 = graphExecutor.exec(obj);
    if (await==null ? config.defaultBlockCompletion():await) {
      var uni2 = completionListener.wait(obj.getId());
      return Uni.combine().all().unis(uni1, uni2)
        .asTuple()
        .flatMap(event -> objectRepo.getAsync(obj.getId()));
    }
    return uni1
      .replaceWith(obj);
  }


  public Uni<OaasObject> submitAndWaitObj(FunctionExecContext ctx, Boolean await) {
    var uni1 = graphExecutor.exec(ctx);
    if (await==null ? config.defaultBlockCompletion():await) {
      var id = ctx.getOutput().getId();
//      var uni2 = completionListener.wait(id);
//      return Uni.combine().all().unis(uni1, uni2)
//        .asTuple()
//        .flatMap(event -> objectRepo.getAsync(id));
      return uni1.flatMap(v -> completionListener.wait(id))
        .flatMap(v -> objectRepo.getAsync(id));
    }
    return uni1
      .replaceWith(ctx.getOutput());
  }

  public Uni<OaasObject> waitObj(OaasObject obj) {
    return completionListener.wait(obj.getId())
      .flatMap(event -> objectRepo.getAsync(obj.getId()));
  }
}
