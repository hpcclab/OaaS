package org.hpcclab.oaas.arango;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;


public class CacheFactory {
  int cacheTimeout;

  public CacheFactory(int cacheTimeout) {
    this.cacheTimeout = cacheTimeout;
  }

  public <V> Cache<String, V> get() {
    return Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMillis(cacheTimeout))
      .build();
  }

  public <V> Cache<String, V> getLongTermVer() {
    return Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMillis(cacheTimeout * 10L))
      .build();
  }
}
