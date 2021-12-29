package org.hpcclab.oaas.taskmanager;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.tm",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface TaskManagerConfig {
  String objectControllerUrl();
  @WithDefault("task-state")
  String stateStoreName();
  @WithDefault("oaas-task-events")
  String taskEventTopic();
  @WithDefault("oaas-tasks")
  String taskTopic();
  @WithDefault("oaas-task-completions")
  String taskCompletionTopic();
  @WithDefault("10")
  Integer taskCompletionPartitions();
  @WithDefault("3")
  int defaultTraverse();
  @WithDefault("true")
  boolean enableCloudEventHeaders();
}
