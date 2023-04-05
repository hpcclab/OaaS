package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;

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

  Uni<V> persistAsync(V v) ;
  default Uni<V> persistWithPreconditionAsync(V v) {
    return persistAsync(v);
  }


  Uni<Void> persistAsync(Collection<V> collection);


  default Uni<Void> persistWithPreconditionAsync(Collection<V> collection) {
    return persistAsync(collection);
  }

  V compute(K key, BiFunction<K, V, V> function);
  Uni<V> computeAsync(K key, BiFunction<K, V, V> function);

  default QueryService<K, V> getQueryService(){
    throw new UnsupportedOperationException();
  }
}
