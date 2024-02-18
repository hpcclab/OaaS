package org.hpcclab.oaas.test;


import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

public class MockInvocationQueueSender implements InvocationQueueProducer {


  public MutableMultimap<String, InvocationRequest> multimap = Multimaps.mutable.list.empty();


  @Override
  public Uni<Void> offer(InvocationRequest request) {
    multimap.put(request.partKey(), request);
    return Uni.createFrom().nullItem();
  }
}
