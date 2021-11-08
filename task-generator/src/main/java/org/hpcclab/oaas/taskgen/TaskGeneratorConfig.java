package org.hpcclab.oaas.taskgen;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "oaas.tg",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface TaskGeneratorConfig {
  String objectControllerUrl();
  @WithDefault("task-state")
  String stateStoreName();
  @WithDefault("oaas-task-events")
  String taskEventTopic();
  @WithDefault("oaas-tasks")
  String taskTopic();
  @WithDefault("oaas-task-completions")
  String taskCompletionTopic();
  @WithDefault("3")
  int defaultTraverse();
  @WithDefault("true")
  boolean enableCloudEventHeaders();
}
