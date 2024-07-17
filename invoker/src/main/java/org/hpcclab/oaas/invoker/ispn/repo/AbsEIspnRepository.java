package org.hpcclab.oaas.invoker.ispn.repo;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.repository.AsyncEntityRepository;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.QueryService;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hpcclab.oaas.repository.ConversionUtils.toUni;

public abstract class AbsEIspnRepository<V> implements EntityRepository<String, V>, AsyncEntityRepository<String, V> {

  protected EIspnAtomicOperationService<V> atomicService;
  protected ArgQueryService<V, ?> queryService;

  abstract String extractKey(V v);

  public abstract AdvancedCache<String, V> getCache();

  @Override
  public EIspnAtomicOperationService<V> atomic() {
    if (atomicService == null)
      atomicService = new EIspnAtomicOperationService<>(this);
    return atomicService;
  }

  @Override
  public AsyncEntityRepository<String, V> async() {
    return this;
  }

  @Override
  public QueryService<String, V> getQueryService() {
    if (queryService == null) throw new UnsupportedOperationException();
    return queryService;
  }

  @Override
  public V get(String key) {
    return getCache().get(key);
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
    return getCache().withFlags(Flag.IGNORE_RETURN_VALUES)
      .put(key, value);
  }

  @Override
  public V persist(V v) {
    return put(extractKey(v), v);
  }

  @Override
  public Uni<V> putAsync(String key, V value) {
    return toUni(getCache()
      .withFlags(Flag.IGNORE_RETURN_VALUES)
      .putAsync(key,value));
  }

  @Override
  public Uni<V> persistAsync(V v) {
    return putAsync(extractKey(v),v);
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection) {
    if (collection.isEmpty()) return Uni.createFrom().nullItem();
    if (collection.size() == 1) {
      V val = collection.iterator().next();
      return persistAsync(val).replaceWithVoid();
    }
    var map = collection.stream()
      .collect(Collectors.toMap(this::extractKey, Function.identity()));
    return toUni(getCache()
      .withFlags(Flag.IGNORE_RETURN_VALUES)
      .putAllAsync(map));
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
