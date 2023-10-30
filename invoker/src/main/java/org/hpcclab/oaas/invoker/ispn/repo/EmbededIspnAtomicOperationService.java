package org.hpcclab.oaas.invoker.ispn.repo;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.hpcclab.oaas.model.exception.DataAccessException;
import org.hpcclab.oaas.repository.AtomicOperationService;
import org.infinispan.util.function.SerializableBiFunction;
import org.infinispan.util.function.SerializableBinaryOperator;
import org.infinispan.util.function.SerializableCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import static org.hpcclab.oaas.repository.ConversionUtils.toUni;

public class EmbededIspnAtomicOperationService<V extends HasKey<String>> implements AtomicOperationService<String, V> {
  private static final Logger logger = LoggerFactory.getLogger(EmbededIspnAtomicOperationService.class);
  AbsEmbeddedIspnRepository<V> repo;
  Random random = new Random();

  public EmbededIspnAtomicOperationService(AbsEmbeddedIspnRepository<V> repository) {
    this.repo = repository;
  }

  @Override
  public Uni<V> persistWithRevAsync(V value) {
    Objects.requireNonNull(value);
    if (!(value instanceof HasRev)) throw new IllegalArgumentException();
    var addRev = random.nextLong(1_000_000);
    var expectRev = Math.abs(((HasRev) value).getRevision() + addRev);
    String key = value.getKey();
    return toUni(repo.getCache().computeAsync(key, (k, oldValue) -> {
      if (oldValue==null) {
        ((HasRev) value).setRevision(expectRev);
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
    })).invoke(Unchecked.consumer(v -> {
      if (((HasRev) v).getRevision()!=expectRev) throw DataAccessException.concurrentMod();
    }));
  }

  @Override
  public Uni<V> persistWithRevAsync(V value, BinaryOperator<V> failureMerger) {
    Objects.requireNonNull(value);
    if (!(value instanceof HasRev)) throw new IllegalArgumentException();
    var addRev = random.nextLong(1_000_000);
    var expectRev = Math.abs(((HasRev) value).getRevision() + addRev);
    String key = value.getKey();
    return toUni(repo.getCache().computeAsync(key, (k, oldValue) -> {
      if (oldValue==null) {
        ((HasRev) value).setRevision(expectRev);
        return value;
      }
      var newRev = ((HasRev) value).getRevision();
      var oldRev = ((HasRev) oldValue).getRevision();
      if (newRev==oldRev) {
        ((HasRev) value).setRevision(expectRev);
        return value;
      } else {
        return failureMerger.apply(oldValue, value);
      }
    }));
  }
}
