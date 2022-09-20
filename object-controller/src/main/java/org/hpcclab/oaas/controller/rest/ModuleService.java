package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
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

@Produces(MediaType.APPLICATION_JSON)
@Path("/api/modules")
public interface ModuleService {

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  Uni<Module> create(Module batch);

  @POST
  @Consumes("text/x-yaml")
  Uni<Module> createByYaml(String body);

  @Data
  @Accessors(chain = true)
  class Module {
    List<OaasClass> classes = List.of();
    List<OaasFunction> functions = List.of();
  }
}
