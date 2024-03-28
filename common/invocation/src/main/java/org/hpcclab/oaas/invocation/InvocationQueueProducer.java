package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

import java.util.Collection;

public interface InvocationQueueProducer {
  Uni<Void> offer(InvocationRequest request);

  default Uni<Void> offer(Collection<InvocationRequest> requests) {
    if (requests.isEmpty()) return Uni.createFrom().nullItem();
    return Multi.createFrom().iterable(requests)
      .onItem().call(this::offer)
      .collect().asList()
      .replaceWithVoid();
  }

}
