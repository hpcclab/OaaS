package org.hpcclab.oaas.pm.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.invocation.service.VertxPackageRoutes;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.pkg.OPackage;

@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/packages")
public class PackageResource {
  final VertxPackageRoutes packageService;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Inject
  public PackageResource(VertxPackageRoutes packageService) {
    this.packageService = packageService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @JsonView(Views.Public.class)
  @RunOnVirtualThread
  public OPackage create(OPackage packageContainer) {
    return packageService.createPackage(packageContainer);
  }

  @POST
  @Consumes("text/x-yaml")
  @RunOnVirtualThread
  public OPackage createByYaml(String body) {
    try {
      var pkg = yamlMapper.readValue(body, OPackage.class);
      return create(pkg);
    } catch (JsonProcessingException e) {
      throw new StdOaasException(e.getMessage(), 400);
    }
  }
}
