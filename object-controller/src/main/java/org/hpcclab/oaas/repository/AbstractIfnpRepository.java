package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractIfnpRepository <K,V>{

  RemoteCache<K,V> remoteCache;
  QueryFactory queryFactory;

  public void setRemoteCache(RemoteCache<K,V> remoteCache) {
    this.remoteCache = remoteCache;
    this.queryFactory = Search.getQueryFactory(remoteCache);
  }

  public RemoteCache<K,V> getRemoteCache(){
    return remoteCache;
  }

  public abstract String getEntityName();

  public List<V> pagination(int page, int size) {
    Query<V> query = (Query<V>) queryFactory.create("FROM "+getEntityName())
      .startOffset((long) page * size)
      .maxResults(size);
    return query.execute().list();
  }

  public V get(K key) {
    Objects.requireNonNull(key);
    return remoteCache.get(key);
  }

  public Uni<V> getAsync(K key) {
    Objects.requireNonNull(key);
    return Uni.createFrom().completionStage(remoteCache.getAsync(key));
  }

  public Map<K, V> list(Set<K> keys) {
    return remoteCache.getAll(keys);
  }

  public Uni<Map<K, V>> listAsync(Set<K> keys) {
    return Uni.createFrom().completionStage(remoteCache
      .getAllAsync(keys));
  }

  public V put(K key, V value) {
    Objects.requireNonNull(key);
    remoteCache.putAsync(key, value);
    return value;
  }

  public Uni<V> putAsync(K key, V value) {
    Objects.requireNonNull(key);
    return Uni.createFrom().completionStage(remoteCache.putAsync(key, value))
      .replaceWith(value);
  }

  public Uni<Void> putAllAsync(Map<K,V> map) {
    Objects.requireNonNull(map);
    return Uni.createFrom().completionStage(remoteCache.putAllAsync(map));
  }

  public Uni<V> removeAsync(K key){
    Objects.requireNonNull(key);
    return Uni.createFrom().completionStage(remoteCache.removeAsync(key));
  }

}
