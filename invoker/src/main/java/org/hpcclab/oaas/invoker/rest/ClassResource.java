package org.hpcclab.oaas.invoker.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.invoker.InvokerManager;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.repository.ObjectRepoManager;

/**
 * @author Pawissanutt
 */
@Path("/api/classes/{cls}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClassResource {
  final InvokerManager invokerManager;
  final HashAwareInvocationHandler hashAwareInvocationHandler;
  final ObjectRepoManager objectRepoManager;

  public ClassResource(InvokerManager invokerManager, HashAwareInvocationHandler hashAwareInvocationHandler, ObjectRepoManager objectRepoManager) {
    this.invokerManager = invokerManager;
    this.hashAwareInvocationHandler = hashAwareInvocationHandler;
    this.objectRepoManager = objectRepoManager;
  }

  @GET
  @Path("invokes/{fb}")
  public Uni<InvocationResponse> invoke(String cls,
                                        String fb,
                                        @Context HttpServerRequest request) {

    return hashAwareInvocationHandler.invoke(ObjectAccessLanguage.builder()
      .cls(cls)
      .fb(fb)
      .build());
  }
}
