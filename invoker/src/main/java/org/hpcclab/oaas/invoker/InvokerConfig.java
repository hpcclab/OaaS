package org.hpcclab.oaas.invoker;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;
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
  @WithDefault("100")
  int connectionPoolMaxSize();
  @WithDefault("10")
  int h2ConnectionPoolMaxSize();
  @WithDefault("1")
  int numOfVerticle();
  @WithDefault("2")
  int numOfInvokerVerticle();
  @WithDefault("600000")
  int invokeTimeout();
  @WithDefault("64")
  int invokeConcurrency();
  @WithDefault("500")
  int maxInflight();
}
