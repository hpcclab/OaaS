package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public interface AtomicOperationService<K, V> {
  Uni<V> persistWithPreconditionAsync(V v);
  default Uni<V> persistWithPreconditionAsync(V value, BinaryOperator<V> failureMerger){
    throw new UnsupportedOperationException();
  }

  default Uni<Void> persistWithPreconditionAsync(Collection<V> collection) {
    return Multi.createFrom().iterable(collection)
      .onItem().transformToUniAndMerge(this::persistWithPreconditionAsync
      )
      .collect().last()
      .replaceWithVoid();
  }

}
