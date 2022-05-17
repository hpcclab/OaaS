package org.hpcclab.oaas.taskmanager.service;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hpcclab.oaas.model.task.OaasTask;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("")
@RegisterRestClient(configKey = "TaskBrokerService")
public interface TaskBrokerService {

  @POST
  @ClientHeaderParam(name = "ce-specversion", value = "1.0")
  @ClientHeaderParam(name = "ce-source", value = "oaas/task-manager")
  @ClientHeaderParam(name = "ce-type", value = "oaas.task")
  @Operation(hidden = true)
  void submitTask(@HeaderParam("ce-id") String id,
                  @HeaderParam("ce-function") String function,
                  @HeaderParam("ce-tasktype") String taskType,
                  OaasTask task);


  @POST
  @ClientHeaderParam(name = "ce-specversion", value = "1.0")
  @ClientHeaderParam(name = "ce-source", value = "oaas/task-manager")
  @ClientHeaderParam(name = "ce-type", value = "oaas.task")
  @Operation(hidden = true)
  Uni<Void> submitTaskAsync(@HeaderParam("ce-id") String id,
                       @HeaderParam("ce-function") String function,
                       @HeaderParam("ce-tasktype") String taskType,
                       OaasTask task);
}
