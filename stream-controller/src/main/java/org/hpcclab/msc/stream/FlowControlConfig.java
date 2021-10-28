package org.hpcclab.msc.stream;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigMapping(prefix = "oaas.flow",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface FlowControlConfig {
  @WithDefault("TaskState")
  String stateStoreName();
  @WithDefault("TaskEvent")
  String taskEventTopic();
}
