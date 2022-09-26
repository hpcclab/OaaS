package org.hpcclab.oaas.invocation;


import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.function.TaskSubmitter;
import org.hpcclab.oaas.model.TaskContext;

import java.util.Map;

public class MockTaskSubmitter implements TaskSubmitter {

  public Map<String, TaskContext> map = Maps.mutable.empty();
  @Override
  public Uni<Void> submit(TaskContext context) {
    map.put(context.getOutput().getId(), context);
    return Uni.createFrom().nullItem();
  }


}
