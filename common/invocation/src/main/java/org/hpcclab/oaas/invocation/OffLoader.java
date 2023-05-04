package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;

public interface OffLoader {
  default Uni<TaskCompletion> offload(OaasTask task) {
    return offload(InvokingDetail.of(task));
  }
  Uni<TaskCompletion> offload(InvokingDetail<?> invokingDetail);
}
