package org.hpcclab.oaas.taskmanager.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;
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
  UnifiedFunctionRouter router;
  @Inject
  ObjectRepository objectRepo;
  @Inject
  InvocationExecutor graphExecutor;
  @Inject
  ObjectCompletionListener completionListener;
  @Inject
  ContentUrlGenerator contentUrlGenerator;
  @Inject
  TaskManagerConfig config;

  @POST
  @JsonView(Views.Public.class)
  public Uni<OaasObject> getObjectWithPost(ObjectAccessLanguage oal,
                                           @QueryParam("await") Boolean await,
                                           @QueryParam("timeout") Integer timeout,
                                           @QueryParam("mq") Boolean mq) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      return applyFunction(oal)
        .flatMap(ctx -> invokeThenAwait(ctx, await, timeout, mq));
    } else {
      return objectRepo.getAsync(oal.getTarget())
        .onItem().ifNull()
        .failWith(() -> StdOaasException.notFoundObject(oal.getTarget(), 404));
    }
  }

  @GET
  @Path("{oal}")
  @JsonView(Views.Public.class)
  public Uni<OaasObject> getObject(@PathParam("oal") String oal,
                                   @QueryParam("await") Boolean await,
                                   @QueryParam("timeout") Integer timeout,
                                   @QueryParam("mq") Boolean mq) {
    var oaeObj = ObjectAccessLanguage.parse(oal);
    LOGGER.debug("Receive OAE getObject '{}'", oaeObj);
    return getObjectWithPost(oaeObj, await, timeout,mq);
  }

  @POST
  @Path("-/{filePath:.*}")
  @JsonView(Views.Public.class)
  public Uni<Response> execAndGetContentPost(@PathParam("filePath") String filePath,
                                             @QueryParam("await") Boolean await,
                                             @QueryParam("timeout") Integer timeout,
                                             @QueryParam("mq") Boolean mq,
                                             ObjectAccessLanguage oal) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      return applyFunction(oal)
        .flatMap(ctx -> invokeThenAwait(ctx, await,  timeout, mq)
          .map(newObj -> createResponse(newObj, filePath))
        );

    } else {
      return objectRepo.getAsync(oal.getTarget())
        .onItem().ifNull()
        .failWith(() -> StdOaasException.notFoundObject(oal.getTarget(), 404))
        .flatMap(obj -> {
          if (obj.isReadyToUsed()) {
            return Uni.createFrom().item(createResponse(obj, filePath));
          }
          if (obj.getStatus().getTaskStatus().isFailed()) {
            return Uni.createFrom().item(createResponse(obj, filePath));
          }
          if (!obj.getStatus().getTaskStatus().isSubmitted()) {
            return awaitCompletion(obj, timeout)
              .map(newObj -> createResponse(newObj, filePath));
          }
          return Uni.createFrom().item(createResponse(obj, filePath));
        });
    }
  }


  @GET
  @JsonView(Views.Public.class)
  @Path("{oal}/{filePath:.*}")
  public Uni<Response> execAndGetContent(@PathParam("oal") String oal,
                                         @PathParam("filePath") String filePath,
                                         @QueryParam("await") Boolean await,
                                         @QueryParam("timeout") Integer timeout,
                                         @QueryParam("mq") Boolean mq) {
    var oaeObj = ObjectAccessLanguage.parse(oal);
    LOGGER.debug("Receive OAL getContent '{}' '{}'", oaeObj, filePath);
    return execAndGetContentPost(filePath, await,  timeout, mq, oaeObj);
  }

  public Uni<FunctionExecContext> applyFunction(ObjectAccessLanguage oal) {
    var uni = router.apply(oal);
    if (LOGGER.isDebugEnabled()) {
      uni = uni
        .invoke(() -> LOGGER.debug("Applying function '{}' succeed", oal));
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
    var fileUrl = contentUrlGenerator.generateUrl(object, filePath, AccessLevel.UNIDENTIFIED);
    return Response.status(redirectCode)
      .location(URI.create(fileUrl))
      .build();
  }


  public Uni<OaasObject> invokeThenAwait(FunctionExecContext ctx,
                                         Boolean await,
                                         Integer timeout,
                                         Boolean mq) {
    if (await==null ? config.defaultAwaitCompletion():await) {
      if (mq==null ? graphExecutor.canSyncInvoke(ctx) : !mq) {
        return graphExecutor.syncExec(ctx)
          .map(tc -> {
            if (tc.getOutput()==null) {
              var main = tc.getMain();
              var completion = tc.getCompletion();
              main.getStatus().set(completion);
              return main;
            }
            return tc.getOutput();
          });
      }

      if (ctx.getOutput()!=null) {
        var id = ctx.getOutput().getId();
        if (completionListener.enabled()) {
          var uni1 = completionListener.wait(id, timeout);
          var uni2 = graphExecutor.exec(ctx);
          return Uni.combine().all().unis(uni1, uni2)
            .asTuple()
            .flatMap(tuple -> objectRepo.getAsync(id));
        } else {
          return graphExecutor.exec(ctx)
            .replaceWith(ctx.getOutput());
        }
      }
    }
    return graphExecutor.exec(ctx)
      .replaceWith(ctx.getOutput() == null? ctx.getMain(): ctx.getOutput());
  }

  public Uni<OaasObject> awaitCompletion(OaasObject obj,
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
