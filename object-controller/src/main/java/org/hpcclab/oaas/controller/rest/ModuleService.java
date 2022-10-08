package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("/api/modules")
public interface ModuleService {

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  Uni<Module> create(@DefaultValue("true")@QueryParam("update") Boolean update,
                     Module batch);

  @POST
  @Consumes("text/x-yaml")
  Uni<Module> createByYaml(@DefaultValue("true")@QueryParam("update") Boolean update,
                           String body);

  @Data
  @Accessors(chain = true)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  class Module {
    String name;
    List<OaasClass> classes = List.of();
    List<OaasFunction> functions = List.of();
  }
}
