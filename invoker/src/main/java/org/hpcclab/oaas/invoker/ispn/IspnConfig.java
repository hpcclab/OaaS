package org.hpcclab.oaas.invoker.ispn;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionConfig;
import org.infinispan.configuration.cache.StorageType;

@ConfigMapping(
  prefix = "oprc.ispn",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface IspnConfig {
  CacheStore objStore();
  CacheStore invStore();
  CacheStore clsStore();
  CacheStore fnStore();
  @WithDefault("-1")
  int hotRodPort();
  interface CacheStore{
    @WithDefault("true")
    boolean persistentEnabled();
    @WithDefault("100000")
    int queueSize();
    @WithDefault("HEAP")
    StorageType storageType();
    @WithDefault("1000000")
    int maxCount();
    @WithDefault("2")
    int owner();
    @WithDefault("30")
    int ttl();
    @WithDefault("false")
    boolean readOnly();
    @WithDefault("true")
    boolean awaitInitialTransfer();

  }
}
