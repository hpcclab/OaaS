package org.hpcclab.oaas.provisioner;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "oaas.kp", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface KpConfig {
  String completionHandlerService();

  @WithDefault("/ce")
  String completionHandlerPath();

  boolean exposeKnative();

  String provisionTopic();
  public static final String LABEL_KEY = "oaas.function";
}
