package org.hpcclab.oaas.invoker.rest;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.repository.InvRepoManager;

@Path("/api/classes/{cls}/invocations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InvocationResource {
  @Inject
  InvRepoManager invRepoManager;

  @GET
  @Path("{key}")
  public Uni<InvocationNode> get(String cls,
                                 String key) {
    return invRepoManager.getOrCreate(cls)
      .async().getAsync(key)
      .onItem().ifNull().failWith(() -> new StdOaasException(404));
  }
}
