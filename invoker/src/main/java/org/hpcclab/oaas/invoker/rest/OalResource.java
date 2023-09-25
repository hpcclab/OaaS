package org.hpcclab.oaas.invoker.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.oal.OalResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@Path("/oal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Startup
public class OalResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(OalResource.class);
  @Inject
  ObjectRepository objectRepo;
  @Inject
  ContentUrlGenerator contentUrlGenerator;
  @Inject
  InvocationReqHandler invocationHandlerService;
  @Inject
  InvokerConfig conf;

  @POST
  @JsonView(Views.Public.class)
  public Uni<OalResponse> getObjectWithPost(ObjectAccessLanguage oal,
                                            @QueryParam("async") Boolean async,
                                            @QueryParam("timeout") Integer timeout) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFb()!=null) {
      return selectAndInvoke(oal, async);
    } else {
      return objectRepo.getAsync(oal.getMain())
        .onItem().ifNull()
        .failWith(() -> StdOaasException.notFoundObject(oal.getMain(), 404))
        .map(obj -> OalResponse.builder()
          .main(obj)
          .build());
    }
  }

  @GET
  @Path("{oal}")
  @JsonView(Views.Public.class)
  public Uni<OalResponse> getObject(@PathParam("oal") String oal,
                                    @QueryParam("async") Boolean async,
                                    @QueryParam("timeout") Integer timeout) {
    var oaeObj = ObjectAccessLanguage.parse(oal);
    LOGGER.debug("Receive OAL getObject '{}'", oaeObj);
    return getObjectWithPost(oaeObj, async, timeout);
  }

  @POST
  @Path("-/{filePath:.*}")
  @JsonView(Views.Public.class)
  public Uni<Response> execAndGetContentPost(@PathParam("filePath") String filePath,
                                             @QueryParam("async") Boolean async,
                                             @QueryParam("timeout") Integer timeout,
                                             ObjectAccessLanguage oal) {
    if (oal==null)
      return Uni.createFrom().failure(BadRequestException::new);
    if (oal.getFb()!=null) {
      return selectAndInvoke(oal, async)
        .map(res -> createResponse(res, filePath));
    } else {
      return objectRepo.getAsync(oal.getMain())
        .onItem().ifNull()
        .failWith(() -> StdOaasException.notFoundObject(oal.getMain(), 404))
        .map(obj -> createResponse(obj, filePath));
    }
  }


  @GET
  @JsonView(Views.Public.class)
  @Path("{oal}/{filePath:.*}")
  public Uni<Response> execAndGetContent(@PathParam("oal") String oal,
                                         @PathParam("filePath") String filePath,
                                         @QueryParam("async") Boolean async,
                                         @QueryParam("timeout") Integer timeout) {
    var oalObj = ObjectAccessLanguage.parse(oal);
    LOGGER.debug("Receive OAL getContent '{}' '{}'", oalObj, filePath);
    return execAndGetContentPost(filePath, async, timeout, oalObj);
  }

  public Uni<OalResponse> selectAndInvoke(ObjectAccessLanguage oal, Boolean async) {
    if (async!=null && async) {
      return invocationHandlerService.asyncInvoke(oal);
    } else {
      return invocationHandlerService.syncInvoke(oal);
    }
  }

  public Response createResponse(OalResponse oalResponse,
                                 String filePath) {
    return createResponse(oalResponse, filePath, HttpResponseStatus.SEE_OTHER.code());
  }


  public Response createResponse(OaasObject object,
                                 String filePath) {
    return createResponse(object, filePath, HttpResponseStatus.SEE_OTHER.code());
  }

  public Response createResponse(OalResponse response,
                                 String filePath,
                                 int redirectCode) {
    var obj = response.output()!=null ? response.output():response.main();
    var ts = response.status();
    if (ts==TaskStatus.DOING) {
      return Response.status(HttpResponseStatus.GATEWAY_TIMEOUT.code())
        .build();
    }
    if (ts.isFailed()) {
      return Response.status(HttpResponseStatus.FAILED_DEPENDENCY.code()).build();
    }
    var oUrl = obj.getState().getOverrideUrls();
    var replaced = oUrl!=null? oUrl.get(filePath): null;
    if (replaced!= null)
      return Response.status(redirectCode)
        .location(URI.create(replaced))
        .build();
    var fileUrl = contentUrlGenerator.generateUrl(obj, filePath, AccessLevel.UNIDENTIFIED, conf.respPubS3());
    return Response.status(redirectCode)
      .location(URI.create(fileUrl))
      .build();
  }

  public Response createResponse(OaasObject object,
                                 String filePath,
                                 int redirectCode) {
    if (object==null) return Response.status(404).build();
    var oUrl = object.getState().getOverrideUrls();
    var replaced = oUrl!=null? oUrl.get(filePath): null;
    if (replaced!= null)
      return Response.status(redirectCode)
        .location(URI.create(replaced))
        .build();
    var fileUrl = contentUrlGenerator.generateUrl(object, filePath, AccessLevel.UNIDENTIFIED, conf.respPubS3());
    return Response.status(redirectCode)
      .location(URI.create(fileUrl))
      .build();
  }
}
