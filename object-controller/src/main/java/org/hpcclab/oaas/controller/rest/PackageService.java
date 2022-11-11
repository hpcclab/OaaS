package org.hpcclab.oaas.controller.rest;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.OaasPackageContainer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/api/packages")
public interface PackageService {

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  Uni<OaasPackageContainer> create(@DefaultValue("true") @QueryParam("update") Boolean update,
                                   OaasPackageContainer oaasPackage);

  @Path("{name}")
  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  Uni<OaasPackageContainer> patch(String name, OaasPackageContainer oaasPackage);

  @Path("{name}")
  @PATCH
  @Consumes("text/x-yaml")
  Uni<OaasPackageContainer> patchByYaml(String name, String body);

  @POST
  @Consumes("text/x-yaml")
  Uni<OaasPackageContainer> createByYaml(@DefaultValue("true") @QueryParam("update") Boolean update,
                                         String body);

}
