package org.hpcclab.oaas.controller;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oprc.pm", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface PkgManagerConfig {
  @WithDefault("oaas-fn")
  String fnProvisionTopic();
  @WithDefault("oaas-cls")
  String clsProvisionTopic();
  @WithDefault("oaas-cr-hash")
  String crHashTopic();
  @WithDefault("true")
  boolean kafkaEnabled();
  @WithDefault("true")
  boolean orbitEnabled();
  String crmHost();
  int crmPort();
}
