package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public interface AsyncEntityRepository<K, V> {

  default Multi<V> values(){
    throw new UnsupportedOperationException();
  }

  Uni<V> getAsync(K key);

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

  Uni<V> removeAsync(K key);


  default Uni<Void> deleteAsync(K key) {
    return removeAsync(key).replaceWithVoid();
  }


  Uni<V> putAsync(K key, V value);

  Uni<V> persistAsync(V v);

  Uni<Void> persistAsync(Collection<V> collection);


  Uni<V> computeAsync(K key, BiFunction<K, V, V> function);


  default QueryService<K, V> getQueryService() {
    throw new UnsupportedOperationException();
  }

  default AtomicOperationService<K, V> atomic() {
    throw new UnsupportedOperationException();
  }

}
