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
  ObjectCache object();
  StateCache state();
  ClassCache cls();
  FunctionCache func();

  interface ObjectCache{
    @WithDefault("false")
    boolean persist();
    @WithDefault("256MB")
    String maxSize();
    @WithDefault("-1")
    int nearCacheMaxEntry();
  }
  interface StateCache{
    @WithDefault("false")
    boolean persist();
    @WithDefault("128MB")
    String maxSize();
    @WithDefault("-1")
    int nearCacheMaxEntry();
  }

  interface ClassCache{
    @WithDefault("1000")
    int nearCacheMaxEntry();
  }
  interface FunctionCache{
    @WithDefault("1000")
    int nearCacheMaxEntry();
  }
}
