package org.hpcclab.oaas.invoker.ispn;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
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
    @WithDefault("4096")
    int queueSize();
    @WithDefault("false")
    boolean async();
    @WithDefault("OFF_HEAP")
    StorageType storageType();
    @WithDefault("1000000")
    Optional<Long> maxCount();
    Optional<String> maxSize();
    @WithDefault("2")
    int owner();
    @WithDefault("false")
    boolean readOnly();
    @WithDefault("false")
    boolean awaitInitialTransfer();
    @WithDefault("1024")
    int transferChuckSize();
    @WithDefault("true")
    boolean fetchInMemoryState();
    @WithDefault("15000")
    long remoteTimeout();
  }
}
