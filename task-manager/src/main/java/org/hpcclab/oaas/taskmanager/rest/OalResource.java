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
import org.hpcclab.oaas.taskmanager.service.InvocationHandlerService;
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
  ObjectRepository objectRepo;
  @Inject
  ContentUrlGenerator contentUrlGenerator;
  @Inject
  TaskManagerConfig config;

  @Inject
  InvocationHandlerService invocationHandlerService;

  @POST
  @JsonView(Views.Public.class)
  public Uni<OaasObject> getObjectWithPost(ObjectAccessLanguage oal,
                                           @QueryParam("await") Boolean await,
                                           @QueryParam("timeout") Integer timeout,
                                           @QueryParam("mq") Boolean mq) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFunctionName()!=null) {
      return selectAndInvoke(oal,await,timeout)
        .map(ctx -> ctx.getOutput()!= null? ctx.getOutput() : ctx.getMain());
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
      return selectAndInvoke(oal,await,timeout)
        .map(ctx -> ctx.getOutput()!= null? ctx.getOutput() : ctx.getMain())
        .map(object -> createResponse(object, filePath));
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
            return invocationHandlerService.awaitCompletion(obj, timeout)
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

  public Uni<FunctionExecContext> selectAndInvoke(ObjectAccessLanguage oal,
                                                  Boolean await,
                                                  Integer timeout){
    if (await==null ? config.defaultAwaitCompletion():await) {
      return invocationHandlerService.syncInvoke(oal);
    } else {
      return invocationHandlerService.asyncInvoke(oal, false, timeout);
    }
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
}
