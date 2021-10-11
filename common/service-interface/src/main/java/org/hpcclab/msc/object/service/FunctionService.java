package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public interface FunctionService {
  @GET
  Uni<List<OaasFunction>> list();

  @POST
  Uni<OaasFunction> create(@DefaultValue("false") @QueryParam("update") boolean update,
                           @Valid OaasFunction function);

  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  Uni<OaasFunction> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                 String body);

  @GET
  @Path("{funcName}")
  Uni<OaasFunction> get(String funcName);
}
