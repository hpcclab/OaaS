package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.TaskCompletion;

public interface OffLoader {
  Uni<TaskCompletion> offload(InvokingDetail<?> invokingDetail);
}
