package org.hpcclab.oaas.orbit;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.om", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OrbitManagerConfig {
  boolean exposeKnative();
  String LABEL_KEY = "oaas.function";
}
