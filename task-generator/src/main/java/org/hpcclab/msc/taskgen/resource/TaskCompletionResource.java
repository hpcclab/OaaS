package org.hpcclab.msc.taskgen.resource;

import io.smallrye.mutiny.Multi;
import org.hpcclab.msc.object.entity.task.TaskCompletion;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.taskgen.repository.TaskCompletionRepository;
import org.hpcclab.msc.taskgen.repository.TaskFlowRepository;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/task-completions")
public class TaskCompletionResource {

  @Inject
  TaskCompletionRepository taskCompletionRepo;

  @GET
  public Multi<TaskCompletion> list() {
    return taskCompletionRepo.findAll()
      .stream();
  }
}
