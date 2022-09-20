package org.hpcclab.oaas.arango;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(
  prefix = "oaas.repo.arg",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface ArgRepositoryConfig {
  @WithDefault("localhost")
  String host();
  @WithDefault("8529")
  int port();
  @WithDefault("root")
  String user();
  Optional<String> pass();
  @WithDefault("oaas")
  String db();
  @WithDefault("OaasObject")
  String objectCollection();
  @WithDefault("OaasFunction")
  String functionCollection();
  @WithDefault("OaasClass")
  String classCollection();
  @WithDefault("OaasObjectDependEdge")
  String odeCollection();
}
