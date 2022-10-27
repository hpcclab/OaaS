package org.hpcclab.oaas.arango.repo;

import com.arangodb.ArangoDBException;
import com.github.benmanes.caffeine.cache.Cache;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.repository.CachedEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class AbstractCachedArgRepository<V> extends AbstractArgRepository<V>
  implements CachedEntityRepository<String, V> {
  private static final Logger LOGGER = LoggerFactory.getLogger( AbstractCachedArgRepository.class );

  abstract Cache<String, V> cache();

  @Override
  public V get(String key) {
    return cache().get(key, super::get);
  }

  @Override
  public Uni<V> getAsync(String key) {
    var val = cache().getIfPresent(key);
    if (val != null)
      return Uni.createFrom().item(get(key));
    return getWithoutCacheAsync(key);
  }


  @Override
  public V getWithoutCache(String key) {
    var v =  super.get(key);
    cache().put(key, v);
    return v;
  }

  @Override
  public Uni<V> getWithoutCacheAsync(String key) {
    return super.getAsync(key)
      .invoke(v -> cache().put(key, v));
  }

  @Override
  public Map<String, V> list(Collection<String> keys) {
    return cache().getAll(keys, kl -> super.list((Collection<String>) kl));
  }

  @Override
  public Uni<Map<String, V>> listAsync(Collection<String> keys) {
    return Uni.createFrom().item(list(keys));
  }

  @Override
  public V remove(String key) {
    cache().invalidate(key);
    return super.remove(key);
  }

  @Override
  public Uni<V> removeAsync(String key) {
    cache().invalidate(key);
    return super.removeAsync(key);
  }

  @Override
  public V put(String key, V value) {
    var v = super.put(key, value);
    cache().put(key, value);
    return v;
  }

  @Override
  public Uni<V> putAsync(String key, V value) {
    return super.putAsync(key, value)
      .invoke(v -> cache().put(key, v));
  }

  @Override
  public Uni<V> persistAsync(V v, boolean notificationEnabled) {
    return super.persistAsync(v, notificationEnabled)
      .invoke(val -> cache().put(extractKey(val), val));
  }

  @Override
  public Uni<Void> persistAsync(Collection<V> collection, boolean notificationEnabled) {
    return super.persistAsync(collection, notificationEnabled)
      .invoke(() -> {
        for (var val: collection) {
          cache().put(extractKey(val), val);
        }
      });
  }

  @Override
  public Uni<V> computeAsync(String key, BiFunction<String, V, V> function) {
    LOGGER.debug("computeAsync(cache)[{}] {}",
      getCollection().name(), key);
    cache().invalidate(key);
    var uni = Uni.createFrom()
      .completionStage(() -> {
        cache().invalidate(key);
        return getAsyncCollection()
            .getDocument(key, getValueCls())
            .thenCompose(doc -> {
              var newDoc = function.apply(key, doc);
              return getAsyncCollection().replaceDocument(key, newDoc, replaceOptions())
                .thenApply(__ -> newDoc);
            });
        }
      )
      .onFailure(ArangoDBException.class)
      .retry().atMost(5)
      .invoke(val -> cache().put(key, val));
    var ctx = Vertx.currentContext();
    if (ctx!=null)
      return uni.emitOn(ctx::runOnContext);
    return uni;
  }

  @Override
  public V compute(String key, BiFunction<String, V, V> function) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("compute(cache)[{}] {}", getCollection().name(), key);
    }
    var retryCount = 5;
    var col = getCollection();
    ArangoDBException exception = null;
    while (retryCount >0) {
      try {
        cache().invalidate(key);
        var doc = col.getDocument(key,getValueCls());
        var newDoc = function.apply(key, doc);
        col.replaceDocument(key, newDoc, replaceOptions());
        cache().put(key,newDoc);
        return newDoc;
      } catch (ArangoDBException e) {
        exception = e;
      }
      retryCount--;
    }
    throw exception;
  }

  @Override
  public void invalidate(String key) {
    cache().invalidate(key);
  }

}
