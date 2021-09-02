package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.service.FunctionService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URI;

@Path("/api/tasks")
public class TaskResource {

  FunctionService functionService;

  @ConfigProperty(name = "objectControllerUrl")
  String objectControllerUrl;

  @PostConstruct
  void setup() {
    this.functionService = RestClientBuilder.newBuilder()
      .baseUri(URI.create(objectControllerUrl))
      .build(FunctionService.class);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<MscFunction> hello() {
    return functionService.get("buildin.logical.copy");
  }
}
