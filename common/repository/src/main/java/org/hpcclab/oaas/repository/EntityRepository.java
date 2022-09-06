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

  Map<K, V> list(Set<K> keys);

  Uni<Map<K, V>> listAsync(Set<K> keys);

  default Uni<List<V>> orderedListAsync(List<K> keys) {
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

  Uni<V> removeAsync(K key);

  V put(K key, V value);

  Uni<V> putAsync(K key, V value);
  Uni<Void> putAllAsync(Map<K, V> map);

  default Uni<V> persistAsync(V v) {
    return persistAsync(v, true);
  }
  Uni<V> persistAsync(V v, boolean notificationEnabled);

  default Uni<Void> persistAsync(Collection<V> collection){
    return persistAsync(collection, true);
  }

  Uni<Void> persistAsync(Collection<V> collection, boolean notificationEnabled);

  Uni<V> computeAsync(K key, BiFunction<K, V, V> function);

  default Pagination<V> pagination(long offset, int limit){
    throw new IllegalStateException();
  }
}
