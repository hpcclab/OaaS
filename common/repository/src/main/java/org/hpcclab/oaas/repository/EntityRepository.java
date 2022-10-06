package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public interface EntityRepository<K, V> {
  V get(K key);

  Uni<V> getAsync(K key);

  Map<K, V> list(Collection<K> keys);

  Uni<Map<K, V>> listAsync(Collection<K> keys);

  default Uni<List<V>> orderedListAsync(Collection<K> keys) {
    if (keys==null || keys.isEmpty()) return Uni.createFrom().item(List.of());
    return this.listAsync(Set.copyOf(keys))
      .map(map -> keys.stream()
        .map(id -> {
          var v = map.get(id);
          if (v==null) throw new IllegalStateException();
          return v;
        })
        .toList()
      );
  }

  V remove(K key);

  Uni<V> removeAsync(K key);

  V put(K key, V value);

  Uni<V> putAsync(K key, V value);
//  Uni<Void> putAllAsync(Map<K, V> map);

  default Uni<V> persistAsync(V v) {
    return persistAsync(v, true);
  }
  Uni<V> persistAsync(V v, boolean notificationEnabled);

  default Uni<Void> persistAsync(Collection<V> collection){
    return persistAsync(collection, true);
  }

  Uni<Void> persistAsync(Collection<V> collection, boolean notificationEnabled);

  V compute(K key, BiFunction<K, V, V> function);
  Uni<V> computeAsync(K key, BiFunction<K, V, V> function);

  Pagination<V> pagination(long offset, int limit);
  Uni<Pagination<V>> paginationAsync(long offset, int limit);

  Uni<Pagination<V>> sortedPaginationAsync(String name,long offset, int limit);

  default Pagination<V> queryPagination(String queryString, long offset, int limit) {
    return queryPagination(queryString, Map.of(), offset, limit);
  }

  List<V> query(String queryString, Map<String, Object> params);
  Uni<List<V>> queryAsync(String queryString, Map<String, Object> params);

  Pagination<V> queryPagination(String queryString, Map<String, Object> params, long offset, int limit);
  Uni<Pagination<V>> queryPaginationAsync(String queryString, Map<String, Object> params, long offset, int limit);


}
