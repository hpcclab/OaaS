package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.object.ObjectConstructResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/object-construct")
public interface ObjectConstructService {
  @POST
  Uni<ObjectConstructResponse> construct(ObjectConstructRequest construction);
}
