package org.hpcclab.oaas.orbit;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oprc.om", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OrbitManagerConfig {
  boolean exposeKnative();
  String clsManagerHost();
  int clsManagerPort();
  String LABEL_KEY = "oaas.function";
}
