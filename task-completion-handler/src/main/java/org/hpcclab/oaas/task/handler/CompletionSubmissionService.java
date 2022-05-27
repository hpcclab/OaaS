package org.hpcclab.oaas.task.handler;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hpcclab.oaas.model.task.TaskCompletion;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/task-completions")
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "taskManagerClient")
public interface CompletionSubmissionService {
  @POST
  Uni<Void> submit(List<TaskCompletion> taskCompletion);
}
