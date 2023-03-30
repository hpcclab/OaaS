package org.hpcclab.oaas.invoker.ispn.store;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

public interface ArgConnectionConfig {
  @WithDefault("localhost")
  String host();
  @WithDefault("8529")
  int port();
  @WithDefault("root")
  String user();
  Optional<String> pass();
  @WithDefault("oaas")
  String db();
}
