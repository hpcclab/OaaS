package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.MscFunction;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public interface FunctionService {
  @GET
  Uni<List<MscFunction>> list();
  @POST
  Uni<MscFunction> create(MscFunction mscFunction);
  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  Uni<MscFunction> createByYaml(String body);
  @GET
  @Path("{funcName}")
  Uni<MscFunction> get(String funcName);
}
