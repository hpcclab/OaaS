package org.hpcclab.oaas.controller;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.oc", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OcConfig {
  String storageAdapterUrl();
  @WithDefault("oaas-fn")
  String fnProvisionTopic();
  @WithDefault("oaas-cls")
  String clsProvisionTopic();
  @WithDefault("true")
  boolean kafkaEnabled();
}
