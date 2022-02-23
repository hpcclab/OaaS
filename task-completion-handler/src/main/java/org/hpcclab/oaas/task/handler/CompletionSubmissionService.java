package org.hpcclab.oaas.task.handler;

import org.hpcclab.oaas.model.proto.TaskCompletion;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/api/task-completions")
public interface CompletionSubmissionService {
  @POST
  void submit(TaskCompletion taskCompletion);
}
