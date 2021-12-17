package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.DeepOaasClassDto;
import org.hpcclab.oaas.model.proto.OaasClassPb;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/classes")
public interface ClassService {
  @GET
  Uni<List<OaasClassPb>> list(@QueryParam("page") Integer page,
                              @QueryParam("size") Integer size);

  @POST
  Uni<OaasClassPb> create(@DefaultValue("false") @QueryParam("update") boolean update,
                           @Valid OaasClassPb classDto);

  @PATCH
  @Path("{name}")
  Uni<OaasClassPb> patch(String name,
                          @Valid OaasClassPb classDto);

  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  Uni<OaasClassPb> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                                 String body);

  @GET
  @Path("{name}")
  Uni<OaasClassPb> get(String name);


  @GET
  @Path("{name}/deep")
  Uni<DeepOaasClassDto> getDeep(String name);


  @DELETE
  @Path("{name}")
  Uni<OaasClassPb> delete(String name);
}
