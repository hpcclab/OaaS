package org.hpcclab.oaas.controller;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.net.URL;
import java.nio.file.Path;

@ConfigMapping(prefix = "oaas.oc", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OcConfig {
  @WithDefault("http://localhost:8088")
  String taskGeneratorUrl();
  @WithDefault("http://localhost:8093")
  String storageAdapterUrl();
  String provisionTopic();
}
