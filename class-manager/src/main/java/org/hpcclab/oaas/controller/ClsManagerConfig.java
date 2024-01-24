package org.hpcclab.oaas.controller;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oprc.cm", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface ClsManagerConfig {
  @WithDefault("oaas-fn")
  String fnProvisionTopic();
  @WithDefault("oaas-cls")
  String clsProvisionTopic();
  @WithDefault("true")
  boolean kafkaEnabled();
  @WithDefault("true")
  boolean orbitEnabled();
  String orbitManagerHost();
  int orbitManagerPort();
}
