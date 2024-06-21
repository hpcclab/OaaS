package org.hpcclab.oaas.invocation.task;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.OTaskCompletion;

public interface OffLoader {
  Uni<OTaskCompletion> offload(InvokingDetail<?> invokingDetail);
}
