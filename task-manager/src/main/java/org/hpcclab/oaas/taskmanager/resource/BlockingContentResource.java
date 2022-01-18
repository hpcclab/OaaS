package org.hpcclab.oaas.taskmanager.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.repository.TaskCompletionRepository;
import org.hpcclab.oaas.taskmanager.service.TaskCompletionListener;
import org.hpcclab.oaas.taskmanager.service.TaskEventManager;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/contents")
public class BlockingContentResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(BlockingContentResource.class);

  @Inject
  TaskCompletionRepository completionRepo;
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  TaskEventManager taskEventManager;
  @Inject
  TaskCompletionListener completionListener;

  @GET
  @Path("{objectId}/{filePath:.*}")
  public Uni<Response> get(String objectId,
                           String filePath) {
    var id = UUID.fromString(objectId);
    return completionRepo.getAsync(id)
      .onItem().ifNull()
      .switchTo(() -> submitAndWait(id))
      .flatMap(taskCompletion -> createResponse(id,filePath, taskCompletion));
  }

  public Uni<Response> createResponse(UUID id,
                                      String filePath,
                                      TaskCompletion taskCompletion) {
    if (taskCompletion==null) {
      return Uni.createFrom().item(
        Response.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build()
      );
    }
    if (taskCompletion.getStatus()!=TaskStatus.SUCCEEDED) {
      return Uni.createFrom().item(
        Response.status(HttpResponseStatus.FAILED_DEPENDENCY.code())
          .build());
    }
    return objectRepo.getAsync(id)
      .map(obj -> createResponse(obj, filePath));
  }

  public Response createResponse(OaasObject obj, String filePath) {
    if (obj == null) return Response.status(404).build();
    var baseUrl = obj.getState().getBaseUrl();
    if (!baseUrl.endsWith("/"))
      baseUrl += '/';
    return Response.status(HttpResponseStatus.FOUND.code())
      .location(URI.create(baseUrl).resolve(filePath))
      .build();
  }


  public Uni<TaskCompletion> submitAndWait(UUID id) {
    var uni1 = taskEventManager.submitCreateEvent(id.toString())
      .onFailure().invoke(e -> LOGGER.error("Got an error when submitting CreateEvent",e));
    var uni2 = completionListener.wait(id);
    return Uni.combine().all().unis(uni1, uni2)
      .asTuple()
      .flatMap(event -> completionRepo.getAsync(id));
  }
}
