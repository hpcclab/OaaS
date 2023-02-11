package org.hpcclab.oaas.test;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MapEntityRepository<K,V> implements EntityRepository<K,V> {
private static final Logger LOGGER = LoggerFactory.getLogger( MapEntityRepository.class );
  MutableMap<K,V> map;
  Function<V,K> keyExtractor;

  public MapEntityRepository(MutableMap<K, V> map,
                             Function<V,K> keyExtractor) {
    this.map = map;
    this.keyExtractor = keyExtractor;
  }

  @Override
  public V get(K key) {
    var v = map.get(key);
    if (v instanceof Copyable)
      return ((Copyable<V>) v).copy();
    return v;
  }

  @Override
  public Uni<V> getAsync(K key) {
    return Uni.createFrom().item(get(key));
  }

  @Override
  public Map<K, V> list(Collection<K> keys) {
    return map.select((k,v) -> keys.contains(k));
  }

  @Override
  public Uni<Map<K, V>> listAsync(Collection<K> keys) {
    return Uni.createFrom().item(list(keys));
  }


  @Override
  public V remove(K key) {
    return map.remove(key);
  }

  @Override
  public Uni<V> removeAsync(K key) {
    return Uni.createFrom().item(map.remove(key));
  }

  @Override
  public V put(K key, V value) {
    if (value instanceof Copyable<?>)
      return map.put(key, ((Copyable<V>) value).copy());
    return map.put(key,value);
  }

  @Override
  public Uni<V> putAsync(K key, V value) {
    return Uni.createFrom().item(put(key, value));
  }

//  @Override
  public Uni<Void> putAllAsync(Map<K, V> m) {
    m.forEach(this::put);
    return Uni.createFrom().voidItem();
  }


  @Override
  public Uni<V> persistAsync(V v, boolean notificationEnabled) {
    LOGGER.debug("persistAsync {}, {}", v, notificationEnabled);
    return putAsync(keyExtractor.apply(v), v);
  }
  @Override
  public Uni<Void> persistAsync(Collection<V> collection, boolean notificationEnabled) {
    LOGGER.debug("persistAsync {}, {}", collection, notificationEnabled);
    var m = Lists.fixedSize.ofAll(collection)
      .groupByUniqueKey(keyExtractor::apply);
    return putAllAsync(m);
//    return Uni.createFrom().voidItem();
  }

  @Override
  public V compute(K key, BiFunction<K, V, V> function) {
    return map.compute(key, (k,v) -> {
      var out = function.apply(k,v);
      if (out instanceof Copyable<?>)
        return ((Copyable<V>) out).copy();
      return out;
    });
  }

  @Override
  public Uni<V> computeAsync(K key, BiFunction<K, V, V> function) {

    return Uni.createFrom().item(compute(key,function));
  }


  @Override
  public Pagination<V> pagination(long offset, int limit) {
    throw new IllegalStateException();
  }

  @Override
  public Pagination<V> queryPagination(String queryString, Map<String, Object> params, long offset, int limit) {
    throw new IllegalStateException();
  }

  @Override
  public Uni<Pagination<V>> paginationAsync(long offset, int limit) {
    throw new IllegalStateException();
  }

  @Override
  public Uni<Pagination<V>> queryPaginationAsync(String queryString, Map<String, Object> params, long offset, int limit) {
    throw new IllegalStateException();
  }

  @Override
  public Uni<Pagination<V>> sortedPaginationAsync(String name, boolean desc, long offset, int limit) {
    throw new IllegalStateException();
  }

  @Override
  public List<V> query(String queryString, Map<String, Object> params) {
    throw new IllegalStateException();
  }

  @Override
  public Uni<List<V>> queryAsync(String queryString, Map<String, Object> params) {
    throw new IllegalStateException();
  }


}
