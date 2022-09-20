package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/classes")
public interface ClassService {
  @GET
  Uni<Pagination<OaasClass>> list(@QueryParam("offset") Long offset,
                                  @QueryParam("limit") Integer limit);

  @GET
  @Path("{name}/objects")
  Pagination<OaasObject> listObject(String name,
                              @QueryParam("offset") Long offset,
                              @QueryParam("limit") Integer limit);

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


//  @GET
//  @Path("{name}/deep")
//  Uni<DeepOaasClass> getDeep(String name);


  @DELETE
  @Path("{name}")
  Uni<OaasClass> delete(String name);
}
