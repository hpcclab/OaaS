package org.hpcclab.oaas.invocation.task;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.TaskCompletion;

public interface OffLoader {
  Uni<TaskCompletion> offload(InvokingDetail<?> invokingDetail);
}
