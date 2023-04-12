package org.hpcclab.oaas.taskmanager.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hpcclab.oaas.model.oal.OalResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;

@RegisterRestClient(configKey = "RemoteInvocationHandler")
@ApplicationScoped
@Path("/api/invocations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface RemoteInvocationHandler {
  @POST
  Uni<OalResponse> invoke(ObjectAccessLanguage objectAccessLanguage);
}
