package org.hpcclab.oaas.invoker.rest;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.oal.OalResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.repository.InvNodeRepository;

@Path("/api/invocations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InvocationResource {

  @Inject
  InvocationReqHandler invocationHandlerService;
  @Inject
  InvNodeRepository invNodeRepo;

  @POST
  public Uni<OalResponse> invoke(ObjectAccessLanguage event) {
    return invocationHandlerService.syncInvoke(event);
  }
}
