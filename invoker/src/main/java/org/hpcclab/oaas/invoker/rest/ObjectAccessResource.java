package org.hpcclab.oaas.invoker.rest;


import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.ObjectRepoManager;

import java.net.URI;

/**
 * @author Pawissanutt
 */
@Path("/api/classes/{cls}/objects/{objId}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ObjectAccessResource {
  final InvokerManager invokerManager;
  final HashAwareInvocationHandler hashAwareInvocationHandler;
  final ObjectRepoManager objectRepoManager;
  final ContentUrlGenerator generator;
  final InvokerConfig conf;

  public ObjectAccessResource(InvokerManager invokerManager,
                              HashAwareInvocationHandler hashAwareInvocationHandler,
                              ObjectRepoManager objectRepoManager, ContentUrlGenerator generator,
                              InvokerConfig conf) {
    this.invokerManager = invokerManager;
    this.hashAwareInvocationHandler = hashAwareInvocationHandler;
    this.objectRepoManager = objectRepoManager;
    this.generator = generator;
    this.conf = conf;
  }

  @GET
  public Uni<OObject> getObj(String cls,
                             String objId) {
    boolean contains = invokerManager.getManagedCls().contains(cls);
    if (contains) {
      return objectRepoManager.getOrCreate(cls).async()
        .getAsync(cls);
    } else {
      return hashAwareInvocationHandler.invoke(ObjectAccessLanguage.builder()
          .cls(cls)
          .main(objId)
          .build())
        .map(InvocationResponse::main);
    }
  }

  @GET
  @Path("files/{file:\\w+}")
  public Uni<Response> getObjectFile(String cls,
                                     String objId,
                                     String file) {
    return getObj(cls, objId)
      .map(obj -> {
        var fileUrl = generator.generateUrl(obj, file, AccessLevel.UNIDENTIFIED, conf.respPubS3());
        return Response.status(HttpResponseStatus.SEE_OTHER.code())
          .location(URI.create(fileUrl))
          .build();
      });
  }

//  @GET
//  @Path("invokes/{fb}")
//  public Uni<Response> invoke(String cls,
//                              String objId,
//                              String fb,
//                              @Context HttpServerRequest request) {
//    boolean managed = invokerManager.getManagedCls().contains(cls);
//    if (managed) {
//
//    } else {
//
//    }
//  }
}
