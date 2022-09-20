package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public interface FunctionService {
  @GET
  @JsonView(Views.Public.class)
  Pagination<OaasFunction> list(@QueryParam("offset") Long offset,
                                @QueryParam("limit") Integer limit);

  @POST
  @JsonView(Views.Public.class)
  Uni<List<OaasFunction>> create(
    @DefaultValue("false") @QueryParam("update") boolean update,
    @Valid List<OaasFunction> function
  );

  @POST
  @Consumes("text/x-yaml")
  @JsonView(Views.Public.class)
  Uni<List<OaasFunction>> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                       String body);

  @GET
  @Path("{funcName}")
  @JsonView(Views.Public.class)
  Uni<OaasFunction> get(String funcName);
}
