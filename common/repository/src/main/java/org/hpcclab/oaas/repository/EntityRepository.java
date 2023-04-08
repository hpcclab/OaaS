package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public interface EntityRepository<K, V> {

  V get(K key);

  default Multi<V> values(){
    throw new UnsupportedOperationException();
  }

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

  default void delete(K key) {
    remove(key);
  }

  default Uni<Void> deleteAsync(K key) {
    return removeAsync(key).replaceWithVoid();
  }

  V put(K key, V value);

  Uni<V> putAsync(K key, V value);

  Uni<V> persistAsync(V v);

  Uni<Void> persistAsync(Collection<V> collection);

  V compute(K key, BiFunction<K, V, V> function);

  Uni<V> computeAsync(K key, BiFunction<K, V, V> function);

  default QueryService<K, V> getQueryService() {
    throw new UnsupportedOperationException();
  }

  default AtomicOperationService<K, V> atomic() {
    throw new UnsupportedOperationException();
  }

}
