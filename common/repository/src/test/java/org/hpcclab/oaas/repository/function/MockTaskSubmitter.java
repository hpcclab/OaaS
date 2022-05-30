package org.hpcclab.oaas.repository.function;


import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.TaskContext;

import java.util.Collection;
import java.util.Map;

public class MockTaskSubmitter implements TaskSubmitter{

  Map<String, TaskContext> map = Maps.mutable.empty();
  @Override
  public Uni<Void> submit(TaskContext context) {
    map.put(context.getOutput().getId(), context);
    return Uni.createFrom().nullItem();
  }


}
