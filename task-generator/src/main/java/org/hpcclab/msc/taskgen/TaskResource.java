package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.model.ObjectStateRequest;
import org.hpcclab.msc.object.model.Task;
import org.hpcclab.msc.object.service.FunctionService;
import org.hpcclab.msc.object.service.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URI;

@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

  private static final Logger LOGGER = LoggerFactory.getLogger( TaskResource.class );

  FunctionService functionService;
  ObjectService objectService;

  @Inject
  TaskFactory taskFactory;

  @Channel("tasks")
  Emitter<Task> tasksEmitter;

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
    LOGGER.info("task");
    return objectService.loadExecutionContext(request.getObjectId())
      .map(context -> taskFactory
        .genTask(request, context.getTarget(), context.getAdditionalInputs(), context.getFunction())
      )
      .invoke(t -> LOGGER.info("task {}", t))
      .call(t -> Uni.createFrom().completionStage(tasksEmitter.send(t)));
  }
}
