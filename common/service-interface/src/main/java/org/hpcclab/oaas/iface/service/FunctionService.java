package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public interface FunctionService {
  @GET
  Uni<List<OaasFunctionPb>> list(@QueryParam("page") Integer page,
                                 @QueryParam("size") Integer size);

  @POST
  Uni<List<OaasFunctionPb>> create(
    @DefaultValue("false") @QueryParam("update") boolean update,
    @Valid List<OaasFunctionPb> function
  );

  @POST
//  @Path("-/yaml")
  @Consumes("text/x-yaml")
  Uni<List<OaasFunctionPb>> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                      String body);

  @GET
  @Path("{funcName}")
  Uni<OaasFunctionPb> get(String funcName);
}
