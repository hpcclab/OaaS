package org.hpcclab.oaas.taskgen.resource;

import io.smallrye.common.annotation.Blocking;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.ObjectResourceRequest;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.taskgen.TaskGeneratorConfig;
import org.hpcclab.oaas.taskgen.TaskEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/resource-requests/")
public class ResourceRequestResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRequestResource.class);

  @Inject
  TaskEventManager taskEventManager;
  @Inject
  TaskGeneratorConfig config;

  @POST
  @Blocking
  public void request(ObjectResourceRequest request) {
    var taskId = OaasTask.createId(request.getOwnerObjectId(), request.getRequestFile());
    taskEventManager.submitEventWithTraversal(taskId,
      config.defaultTraverse(),
      true,
      TaskEvent.Type.CREATE);
  }
}
