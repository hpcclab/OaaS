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

  @WithDefault("oaas-fn-")
  String functionTopicPrefix();
  String storageAdapterUrl();
  String natsUrls();

  @WithDefault("100")
  int connectionPoolMaxSize();
  @WithDefault("5")
  int h2ConnectionPoolMaxSize();
  @WithDefault("1")
  int numOfVerticle();
}
