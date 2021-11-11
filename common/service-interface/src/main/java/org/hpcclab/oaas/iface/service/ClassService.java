package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClassDto;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/classes")
public interface ClassService {
  @GET
  Uni<List<OaasClassDto>> list();

  @POST
  Uni<OaasClassDto> create(@DefaultValue("false") @QueryParam("update") boolean update,
                           @Valid OaasClassDto classDto);

  @PATCH
  @Path("{name}")
  Uni<OaasClassDto> patch(String name,
                          @Valid OaasClassDto classDto);

  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  Uni<OaasClassDto> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                 String body);

  @GET
  @Path("{name}")
  Uni<OaasClassDto> get(String name);


  @DELETE
  @Path("{name}")
  Uni<OaasClassDto> delete(String name);
}
