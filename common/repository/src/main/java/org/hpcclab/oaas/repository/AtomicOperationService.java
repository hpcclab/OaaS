package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.function.BinaryOperator;

public interface AtomicOperationService<K, V> {
  Uni<V> persistWithRevAsync(V v);
  default Uni<V> persistWithRevAsync(V value, BinaryOperator<V> failureMerger){
    throw new UnsupportedOperationException();
  }

  default Uni<Void> persistWithRevAsync(Collection<V> collection) {
    return Multi.createFrom().iterable(collection)
      .onItem().transformToUniAndMerge(this::persistWithRevAsync
      )
      .collect().last()
      .replaceWithVoid();
  }

}
