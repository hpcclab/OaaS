package org.hpcclab.oaas.invoker.rest;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.hpcclab.oaas.model.oal.OalResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;

@Path("/api/invocations")
public class InvocationResource {
  @POST
  public Uni<OalResponse> invoke(ObjectAccessLanguage event) {
    return null;
  }


}
