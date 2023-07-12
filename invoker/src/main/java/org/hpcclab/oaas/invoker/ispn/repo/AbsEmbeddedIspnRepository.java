package org.hpcclab.oaas.invoker.ispn.repo;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.repository.AtomicOperationService;
import org.hpcclab.oaas.repository.DefaultAtomicOperationService;
import org.hpcclab.oaas.repository.EntityRepository;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hpcclab.oaas.repository.ConversionUtils.toUni;

public abstract class AbsEmbeddedIspnRepository<V extends HasKey<String>> implements EntityRepository<String, V> {

  protected AtomicOperationService<String, V> atomicService;

  abstract AdvancedCache<String, V> getCache();

  @Override
  public AtomicOperationService<String, V> atomic() {
    if (atomicService == null)
      atomicService = new DefaultAtomicOperationService<>(this);
    return atomicService;
  }

  @Override
  public V get(String key) {
    return getCache().get(key);
  }

  @Override
  public Multi<V> values() {
    return Multi.createFrom().items(getCache().values().stream());
  }

  @Override
  public Uni<V> getAsync(String key) {
    return toUni(getCache().getAsync(key));
  }

  @Override
  public Map<String, V> list(Collection<String> keys) {
    return getCache().getAll(Set.copyOf(keys));
  }

  @Override
  public Uni<Map<String, V>> listAsync(Collection<String> keys) {
    return toUni(getCache().getAllAsync(Set.copyOf(keys)));
  }

  @Override
  public V remove(String key) {
    return getCache().remove(key);
  }

  @Override
  public Uni<V> removeAsync(String key) {
    return toUni(getCache().removeAsync(key));
  }

  @Override
  public void delete(String key) {
    getCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(key);
  }

  @Override
  public Uni<Void> deleteAsync(String key) {
    return toUni(getCache()
      .withFlags(Flag.IGNORE_RETURN_VALUES).removeAsync(key))
      .replaceWithVoid();
  }

  @Override
  public V put(String key, V value) {
    return getCache().put(key, value);
  }

  @Override
  public Uni<V> putAsync(String key, V value) {
    return toUni(getCache().putAsync(key,value));
  }

  @Override
  public Uni<V> persistAsync(V v) {
    return putAsync(v.getKey(),v);
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection) {
    var map = collection.stream()
      .collect(Collectors.toMap(HasKey::getKey, Function.identity()));

    return toUni(getCache().putAllAsync(map));
  }

  @Override
  public V compute(String key, BiFunction<String, V, V> function) {
    return getCache().compute(key, function);
  }

  @Override
  public Uni<V> computeAsync(String key, BiFunction<String, V, V> function) {
    return toUni(getCache().computeAsync(key, function));
  }
}
