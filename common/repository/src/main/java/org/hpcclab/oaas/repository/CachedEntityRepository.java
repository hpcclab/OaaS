package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;

public interface CachedEntityRepository<K,V> extends EntityRepository<K,V> {
  void invalidate(K key);
  V getBypassCache(K key);
  Uni<V> getBypassCacheAsync(K key);
}
