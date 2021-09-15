package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskResource.class);

  @Inject
  TaskHandler taskHandler;

  @POST
  public Uni<Task> task(ObjectResourceRequest request) {
    return taskHandler.handle(request);
  }
}
