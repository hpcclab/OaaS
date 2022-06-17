package org.hpcclab.oaas.repository.init;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(
  prefix = "oaas.repo",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface RepositoryConfig {
  @WithDefault("true")
  boolean createOnStart();
  ObjectConfig object();
  GraphConfig graph();
  ClassConfig cls();
  FunctionConfig func();

  interface ObjectConfig{
    @WithDefault("false")
    boolean persist();
    @WithDefault("256MB")
    String maxSize();
    @WithDefault("-1")
    int nearCacheMaxEntry();
    @WithDefault("8192")
    int writeBackQueueSize();
    @WithDefault("false")
    boolean useRockdb();
  }

  interface GraphConfig{
    @WithDefault("false")
    boolean persist();
    @WithDefault("256MB")
    String maxSize();
  }

  interface ClassConfig{
    @WithDefault("1000")
    int nearCacheMaxEntry();
    @WithDefault("256MB")
    String maxSize();
  }
  interface FunctionConfig{
    @WithDefault("1000")
    int nearCacheMaxEntry();
    @WithDefault("256MB")
    String maxSize();
  }
}
