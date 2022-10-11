package org.hpcclab.oaas.invoker;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Set;

@ConfigMapping(
  prefix = "oaas.invoker",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface InvokerConfig {
  String kafka();
  @WithDefault("oaas-invoker")
  String kafkaGroup();
  Set<String> topics();
  String storageAdapterUrl();
  String natsUrls();
}
