package org.hpcclab.oaas.taskmanager;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.tm",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface TaskManagerConfig {
  String storageAdapterUrl();
  @WithDefault("true")
  boolean defaultAwaitCompletion();
  @WithDefault("100")
  int connectionPoolMaxSize();
  @WithDefault("10")
  int h2ConnectionPoolMaxSize();

  @WithDefault("oaas-fn-")
  String functionTopicPrefix();
}
