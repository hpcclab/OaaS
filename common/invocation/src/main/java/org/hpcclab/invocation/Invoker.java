package org.hpcclab.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;

public interface Invoker {
  Uni<TaskCompletion> invoke(OaasTask task);
}
