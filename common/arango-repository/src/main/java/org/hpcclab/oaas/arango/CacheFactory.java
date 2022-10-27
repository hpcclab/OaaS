package org.hpcclab.oaas.arango;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.function.Supplier;

@ApplicationScoped
public class CacheFactory {

  @Inject
  ArgRepositoryConfig config;

  public <V> Cache<String, V> get() {
    return Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMillis(config.cacheTimeout()))
      .build();
  }
}
