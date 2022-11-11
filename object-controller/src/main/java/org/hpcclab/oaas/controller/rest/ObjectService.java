package org.hpcclab.oaas.controller.rest;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.object.OaasObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public interface ObjectService {
  @GET
  Uni<Pagination<OaasObject>> list(@QueryParam("offset") Integer offset,
                                   @QueryParam("limit") Integer limit,
                                   @QueryParam("sort") String sort);
  @GET
  @Path("{id}")
  Uni<OaasObject> get(String id);

  @DELETE
  @Path("{id}")
  Uni<OaasObject> delete(String id);
}
