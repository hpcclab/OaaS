package org.hpcclab.oaas.invoker.ispn.repo;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.hpcclab.oaas.model.exception.DataAccessException;
import org.hpcclab.oaas.repository.AtomicOperationService;

import java.util.Random;

public class IspnAtomicService<V extends HasKey> implements AtomicOperationService<String, V> {
  AbsEmbeddedIspnRepository<V> repository;
  Random random = new Random();

  public IspnAtomicService(AbsEmbeddedIspnRepository<V> repository) {
    this.repository = repository;
  }

  @Override
  public Uni<V> persistWithPreconditionAsync(V value) {
    if (!(value instanceof HasRev))
      throw new IllegalArgumentException();
    var addRev = random.nextLong(1_000_000);
    var expectRev = Math.abs(((HasRev) value).getRevision() + addRev);
    return repository.computeAsync(value.getKey(), (k, oldValue) -> {
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
