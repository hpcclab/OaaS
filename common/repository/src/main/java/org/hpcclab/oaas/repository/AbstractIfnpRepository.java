package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractIfnpRepository<K, V> {

  RemoteCache<K, V> remoteCache;
  QueryFactory queryFactory;

  public RemoteCache<K, V> getRemoteCache() {
    return remoteCache;
  }

  public void setRemoteCache(RemoteCache<K, V> remoteCache) {
    this.remoteCache = remoteCache;
    this.queryFactory = Search.getQueryFactory(remoteCache);
  }

  public abstract String getEntityName();

  public List<V> pagination(int page, int size) {
    return query("FROM " + getEntityName(), page, size);
  }

  public List<V> query(String queryString, int page, int size) {
    return query(queryString, Map.of(), page, size);
  }

  public List<V> query(String queryString, Map<String, Object> params, int page, int size) {
    Query<V> query = (Query<V>) queryFactory.create(queryString)
      .setParameters(params)
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
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(remoteCache.getAsync(key));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public Map<K, V> list(Set<K> keys) {
    return remoteCache.getAll(keys);
  }

  public Uni<Map<K, V>> listAsync(Set<K> keys) {
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(remoteCache
      .getAllAsync(keys));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public V put(K key, V value) {
    Objects.requireNonNull(key);
    remoteCache.putAsync(key, value);
    return value;
  }

  public Uni<V> putAsync(K key, V value) {
    Objects.requireNonNull(key);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(remoteCache.putAsync(key, value))
      .replaceWith(value);
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public Uni<Void> putAllAsync(Map<K, V> map) {
    Objects.requireNonNull(map);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(remoteCache.putAllAsync(map));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

  public Uni<V> removeAsync(K key) {
    Objects.requireNonNull(key);
    var ctx = Vertx.currentContext();
    var uni = Uni.createFrom().completionStage(remoteCache.removeAsync(key));
    if (ctx!=null)
      uni = uni.emitOn(ctx::runOnContext);
    return uni;
  }

}
