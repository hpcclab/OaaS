package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.DeepOaasClass;
import org.hpcclab.oaas.model.proto.OaasClass;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/classes")
public interface ClassService {
  @GET
  Uni<List<OaasClass>> list(@QueryParam("page") Integer page,
                            @QueryParam("size") Integer size);

  @POST
  Uni<OaasClass> create(@DefaultValue("false") @QueryParam("update") boolean update,
                        @Valid OaasClass classDto);

  @PATCH
  @Path("{name}")
  Uni<OaasClass> patch(String name,
                       @Valid OaasClass classDto);

  @POST
  @Consumes("text/x-yaml")
  @Produces(MediaType.APPLICATION_JSON)
  Uni<OaasClass> createByYaml(@DefaultValue("false") @QueryParam("update") boolean update,
                              String body);

  @GET
  @Path("{name}")
  Uni<OaasClass> get(String name);


  @GET
  @Path("{name}/deep")
  Uni<DeepOaasClass> getDeep(String name);


  @DELETE
  @Path("{name}")
  Uni<OaasClass> delete(String name);
}
