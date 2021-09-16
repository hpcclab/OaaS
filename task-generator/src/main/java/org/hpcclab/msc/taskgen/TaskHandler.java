package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.model.Task;
import org.hpcclab.msc.object.service.FunctionService;
import org.hpcclab.msc.object.service.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.net.URI;

@ApplicationScoped
public class TaskHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskHandler.class );

  @Inject
  TaskFactory taskFactory;

  @Channel("tasks")
  Emitter<Task> tasksEmitter;

  FunctionService functionService;
  ObjectService objectService;

  @ConfigProperty(name = "oaas.tg.objectControllerUrl")
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

  public Uni<Task> handle(ObjectResourceRequest request) {
    if (request.getOwnerObjectId() == null || !ObjectId.isValid(request.getOwnerObjectId())) {
      LOGGER.warn("receive request with invalid ownerObjectId. [{}]",request);
      return Uni.createFrom().nullItem();
    }
    return objectService.loadExecutionContext(request.getOwnerObjectId())
      .map(context -> taskFactory
        .genTask(request, context.getTarget(), context.getAdditionalInputs(), context.getFunction())
      )
      .invoke(t -> LOGGER.info("task {}", t))
      .call(t -> Uni.createFrom().completionStage(tasksEmitter.send(t)))
      .onFailure(f -> {
        if (f instanceof WebApplicationException exception) {
          return exception.getResponse().getStatus() == 404;
        }
        return false;
      })
      .recoverWithNull();
  }
}
