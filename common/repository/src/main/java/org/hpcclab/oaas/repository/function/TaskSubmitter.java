package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;

public interface TaskSubmitter {
  Uni<Void> submit(TaskContext context);
}
