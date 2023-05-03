package org.hpcclab.oaas.test;


import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.hpcclab.oaas.invocation.InvocationQueueSender;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

public class MockInvocationQueueSender implements InvocationQueueSender {

  TaskFactory taskFactory;

  public MockInvocationQueueSender(TaskFactory taskFactory) {
    this.taskFactory = taskFactory;
  }

  public MutableMultimap<String, InvocationRequest> multimap = Multimaps.mutable.list.empty();


  @Override
  public Uni<Void> send(InvocationRequest request) {
    multimap.put(request.partKey(), request);
    return Uni.createFrom().nullItem();
  }
}
