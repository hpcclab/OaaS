package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.hpcclab.oaas.model.exception.DataAccessException;

import java.util.Random;

public class DefaultAtomicOperationService<K,V extends HasKey<K>> implements AtomicOperationService<K,V>{
  Random random = new Random();
  EntityRepository<K,V> repository;

  public DefaultAtomicOperationService(EntityRepository<K, V> repository) {
    this.repository = repository;
  }

  @Override
  public Uni<V> persistWithPreconditionAsync(V value) {
    if (!(value instanceof HasRev))
      throw new IllegalArgumentException();
    var addRev = random.nextLong(1_000_000);
    var expectRev = Math.abs(((HasRev) value).getRevision() + addRev);
    K key = value.getKey();
    return repository.computeAsync(key, (k, oldValue) -> {
        if (oldValue == null) {
          ((HasRev)value).setRevision(expectRev);
          return value;
        }
        var newRev = ((HasRev) value).getRevision();
        var oldRev = ((HasRev) oldValue).getRevision();
        if (newRev==oldRev) {
          ((HasRev) value).setRevision(expectRev);
          return value;
        } else {
          return oldValue;
        }
      })
      .invoke(Unchecked.consumer(v -> {
        if (((HasRev) v).getRevision()!=expectRev)
          throw DataAccessException.concurrentMod();
      }));
  }
}
