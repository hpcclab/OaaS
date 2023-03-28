package org.hpcclab.oaas.arango;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;

@ApplicationScoped
public class CacheFactory {

  @Inject
  ArgRepositoryConfig config;

  public <V> Cache<String, V> get() {
    return Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMillis(config.cacheTimeout()))
      .build();
  }

  public <V> Cache<String, V> getLongTermVer() {
    return Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMillis(config.cacheTimeout() * 10L))
      .build();
  }
}
