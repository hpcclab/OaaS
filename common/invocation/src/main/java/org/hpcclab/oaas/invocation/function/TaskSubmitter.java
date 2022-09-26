package org.hpcclab.oaas.invocation.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;

import java.util.Collection;

public interface TaskSubmitter {
  Uni<Void> submit(TaskContext context);

  default Uni<Void> submit(Collection<TaskContext> contexts) {
    return Multi.createFrom().iterable(contexts)
      .onItem().call(this::submit)
      .collect().last()
      .replaceWithVoid();
  }

}
