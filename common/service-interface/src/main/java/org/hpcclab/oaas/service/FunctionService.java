package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.OaasFunctionDto;

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
  Uni<List<OaasFunctionDto>> create(
    @DefaultValue("false") @QueryParam("update") boolean update,
    @Valid List<OaasFunctionDto> function
  );

  @POST
//  @Path("-/yaml")
  @Consumes("text/x-yaml")
  Uni<List<OaasFunctionDto>> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                      String body);

  @GET
  @Path("{funcName}")
  Uni<OaasFunctionDto> get(String funcName);
}
