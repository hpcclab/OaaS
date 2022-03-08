package org.hpcclab.oaas.task.handler;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hpcclab.oaas.model.proto.TaskCompletion;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

@Path("/api/task-completions")
@RegisterRestClient(configKey = "taskManagerClient")
public interface CompletionSubmissionService {
  @POST
  Uni<Void> submit(List<TaskCompletion> taskCompletion);
}
