package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.task.TaskCompletion;

public class MockSyncInvoker implements SyncInvoker{
  @Override
  public Uni<TaskCompletion> invoke(InvokingDetail<?> invokingDetail) {
    //TODO
    return Uni.createFrom().nullItem();
  }

  @Override
  public Uni<TaskCompletion> invoke(TaskContext taskContext) {
    //TODO
    return Uni.createFrom().nullItem();
  }
}
