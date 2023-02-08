package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;

public interface SyncInvoker {
  default Uni<TaskCompletion> invoke(OaasTask task) {
    return invoke(InvokingDetail.of(task));
  }
  Uni<TaskCompletion> invoke(InvokingDetail<?> invokingDetail);
}
