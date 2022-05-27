package org.hpcclab.oaas.provisioner;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "oaas.kp", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface KpConfig {
  @WithName("taskHandlerService")
  String taskHandler();

  @WithDefault("/")
  String taskHandlerPath();

  boolean exposeKnative();

  @WithDefault("true")
  boolean addAffinity();

  String provisionTopic();
}
