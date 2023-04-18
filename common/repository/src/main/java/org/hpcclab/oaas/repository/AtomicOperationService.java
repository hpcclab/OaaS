package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Collection;

public interface AtomicOperationService<K, V> {
  Uni<V> persistWithPreconditionAsync(V v);

  default Uni<Void> persistWithPreconditionAsync(Collection<V> collection) {
    return Multi.createFrom().iterable(collection)
      .onItem().transformToUniAndMerge(this::persistWithPreconditionAsync
      )
      .collect().last()
      .replaceWithVoid();
  }
}
