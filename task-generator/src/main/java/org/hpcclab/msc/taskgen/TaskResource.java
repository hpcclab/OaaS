package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.model.ObjectStateRequest;
import org.hpcclab.msc.object.model.Task;
import org.hpcclab.msc.object.service.FunctionService;
import org.hpcclab.msc.object.service.ObjectService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URI;

@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

  FunctionService functionService;
  ObjectService objectService;

  @Inject
  TaskGenerator taskGenerator;

  @ConfigProperty(name = "objectControllerUrl")
  String objectControllerUrl;

  @PostConstruct
  void setup() {
    this.functionService = RestClientBuilder.newBuilder()
      .baseUri(URI.create(objectControllerUrl))
      .build(FunctionService.class);
    this.objectService = RestClientBuilder.newBuilder()
      .baseUri(URI.create(objectControllerUrl))
      .build(ObjectService.class);
  }

  @GET
  public Uni<MscFunction> hello() {
    return functionService.get("buildin.logical.copy");
  }

  @POST
  public Uni<Task> task(ObjectStateRequest request) {
    return objectService.get(request.getObjectId())
      .flatMap(o -> {
        if (o == null) throw new NoStackException("Not found object").setCode(404);
        if (o.getOrigin().getRoot()) return Uni.createFrom().nullItem();
        return functionService.get(o.getOrigin().getFuncName())
          .map(f -> taskGenerator.genTask(request, o, f));
      });
  }
}
