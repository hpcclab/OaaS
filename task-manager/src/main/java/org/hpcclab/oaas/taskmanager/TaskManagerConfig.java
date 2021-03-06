package org.hpcclab.oaas.taskmanager;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.tm",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface TaskManagerConfig {
  String objectControllerUrl();
  String storageAdapterUrl();
  @WithDefault("oaas-task-completions")
  String taskCompletionTopic();
  @WithDefault("10")
  Integer taskCompletionPartitions();
  @WithDefault("3")
  int defaultTraverse();
//  @WithDefault("true")
//  boolean enableCloudEventHeaders();
  String brokerUrl();
  @WithDefault("300")
  int blockingTimeout();
  @WithDefault("true")
  boolean enableCompletionListener();
  @WithDefault("true")
  boolean defaultBlockCompletion();
}
