package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;

public interface SyncInvoker {
  Uni<TaskCompletion> invoke(OaasTask task);
  Uni<TaskCompletion> invoke(TaskContext taskContext);
}
