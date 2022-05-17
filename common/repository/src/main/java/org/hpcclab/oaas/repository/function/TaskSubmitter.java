package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.NotImplementedException;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;

import java.util.Collection;

public interface TaskSubmitter {
  //  Uni<Void> submit(TaskContext context);
  Uni<Void> submit(Collection<TaskContext> contexts);

}
