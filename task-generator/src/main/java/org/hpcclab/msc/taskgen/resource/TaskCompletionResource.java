package org.hpcclab.msc.taskgen.resource;

import io.smallrye.mutiny.Multi;
import org.hpcclab.oaas.entity.task.TaskCompletion;
import org.hpcclab.msc.taskgen.repository.TaskCompletionRepository;

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
