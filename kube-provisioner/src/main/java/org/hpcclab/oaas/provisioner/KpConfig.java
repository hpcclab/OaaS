package org.hpcclab.oaas.provisioner;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.kp", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface KpConfig {
  String completionHandlerService();

  @WithDefault("/ce")
  String completionHandlerPath();

  boolean exposeKnative();

  String fnProvisionTopic();
  String clsProvisionTopic();

  @WithDefault("oaas-invoke-")
  String invokeTopicPrefix();
  String LABEL_KEY = "oaas.function";
}
