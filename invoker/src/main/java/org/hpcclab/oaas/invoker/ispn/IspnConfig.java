package org.hpcclab.oaas.invoker.ispn;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.hpcclab.oaas.invoker.ispn.store.ArgConnectionConfig;
import org.infinispan.configuration.cache.StorageType;

import java.util.Optional;

@ConfigMapping(
  prefix = "oprc.ispn",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface IspnConfig {
  CacheStore objStore();
  @WithDefault("-1")
  int hotRodPort();
  interface CacheStore{
    @WithDefault("1000000")
    int queueSize();
    @WithDefault("false")
    boolean async();
    @WithDefault("HEAP")
    StorageType storageType();
    @WithDefault("1000000")
    Optional<Long> maxCount();
    Optional<String> maxSize();
    @WithDefault("2")
    int owner();
    @WithDefault("-1")
    int ttl();
    @WithDefault("false")
    boolean readOnly();
    @WithDefault("false")
    boolean awaitInitialTransfer();
  }
}
