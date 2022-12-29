package org.hpcclab.oaas.invocation;


import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.function.TaskSubmitter;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.task.OaasTask;

import java.util.Map;

public class MockTaskSubmitter implements TaskSubmitter {

  TaskFactory taskFactory;

  public MockTaskSubmitter(TaskFactory taskFactory) {
    this.taskFactory = taskFactory;
  }

  public Map<String, OaasTask> map = Maps.mutable.empty();
  @Override
  public Uni<Void> submit(TaskContext context) {
    var task = taskFactory.genTask(context);
    map.put(task.getId(), task);
    return Uni.createFrom().nullItem();
  }


}
