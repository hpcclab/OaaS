package org.hpcclab.msc.taskgen.resource;

import io.smallrye.mutiny.Multi;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.taskgen.repository.TaskFlowRepository;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/flows")
public class TaskFlowResource  {

  @Inject
  TaskFlowRepository taskFlowRepo;

  @GET
  public Multi<TaskFlow> list() {
    return taskFlowRepo.findAll()
      .stream();
  }
}
