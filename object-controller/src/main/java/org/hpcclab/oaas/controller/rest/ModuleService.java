package org.hpcclab.oaas.controller.rest;

import io.smallrye.mutiny.Uni;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/modules")
public interface ModuleService {

  @POST
  Uni<Module> create(Module batch);

  @POST
  @Consumes("text/x-yaml")
  Uni<Module> createByYaml(String body);

  @Data
  @Accessors(chain = true)
  public static class Module {
    List<OaasClass> classes = List.of();
    List<OaasFunction> functions = List.of();
  }
}
