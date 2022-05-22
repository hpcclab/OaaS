package org.hpcclab.oaas.controller;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.oc", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OcConfig {
  String storageAdapterUrl();
  String provisionTopic();
  @WithDefault("true")
  boolean kafkaEnabled();
}
