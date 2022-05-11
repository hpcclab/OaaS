package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.proto.OaasFunction;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public interface FunctionService {
  @GET
  Pagination<OaasFunction> list(@QueryParam("offset") Long offset,
                                @QueryParam("limit") Integer limit);

  @POST
  Uni<List<OaasFunction>> create(
    @DefaultValue("false") @QueryParam("update") boolean update,
    @Valid List<OaasFunction> function
  );

  @POST
  @Consumes("text/x-yaml")
  Uni<List<OaasFunction>> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                       String body);

  @GET
  @Path("{funcName}")
  Uni<OaasFunction> get(String funcName);
}
