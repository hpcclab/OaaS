package org.hpcclab.oaas;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.net.URL;
import java.nio.file.Path;

@ConfigMapping(prefix = "oaas.oc", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OcConfig {
  String s3PrefixUrl();
  @WithDefault("http://localhost:8088")
  String taskGeneratorUrl();
  String provisionTopic();
}
