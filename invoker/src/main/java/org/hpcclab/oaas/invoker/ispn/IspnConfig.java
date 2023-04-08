package org.hpcclab.oaas.invoker.ispn;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionConfig;
import org.infinispan.configuration.cache.StorageType;

@ConfigMapping(
  prefix = "oaas.ispn",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface IspnConfig {
  CacheStore objStore();
  CacheStore clsStore();
  CacheStore fnStore();
  @WithDefault("-1")
  int hotRodPort();
  ArgConnectionConfig argConnection();
  interface CacheStore{
    @WithDefault("100000")
    int queueSize();
    @WithDefault("HEAP")
    StorageType storageType();
    @WithDefault("1000000")
    int maxCount();
    @WithDefault("30")
    int ttl();
  }
}
