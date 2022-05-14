package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;

import java.util.Collection;

public interface TaskSubmitter {
  Uni<Void> submit(TaskContext context);
  Uni<Void> submit(Collection<TaskContext> context);
}
