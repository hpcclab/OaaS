package org.hpcclab.oaas.taskmanager;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.tm",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface TaskManagerConfig {
  String storageAdapterUrl();
  @WithDefault("oaas-tasks")
  String taskTopic();
  String brokerUrl();
  @WithDefault("300")
  int blockingTimeout();
  @WithDefault("true")
  boolean enableCompletionListener();
  @WithDefault("true")
  boolean defaultAwaitCompletion();
  String natsUrls();
}
