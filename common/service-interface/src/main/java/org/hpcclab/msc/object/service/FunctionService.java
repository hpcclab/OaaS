package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.model.OaasFunctionDto;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public interface FunctionService {
  @GET
  Uni<List<OaasFunctionDto>> list();

  @POST
  Uni<OaasFunctionDto> create(@DefaultValue("false") @QueryParam("update") boolean update,
                           @Valid OaasFunctionDto function);

  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  Multi<OaasFunctionDto> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                      String body);

  @GET
  @Path("{funcName}")
  Uni<OaasFunctionDto> get(String funcName);
}
