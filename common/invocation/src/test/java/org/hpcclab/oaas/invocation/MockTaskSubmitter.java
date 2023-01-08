package org.hpcclab.oaas.invocation;


import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.hpcclab.oaas.invocation.function.TaskSubmitter;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.task.OaasTask;

public class MockTaskSubmitter implements TaskSubmitter {

  TaskFactory taskFactory;

  public MockTaskSubmitter(TaskFactory taskFactory) {
    this.taskFactory = taskFactory;
  }

  public MutableMultimap<String, OaasTask> multimap = Multimaps.mutable.list.empty();
  @Override
  public Uni<Void> submit(TaskContext context) {
    var task = taskFactory.genTask(context);
    multimap.put(task.getId(), task);
    return Uni.createFrom().nullItem();
  }


}
