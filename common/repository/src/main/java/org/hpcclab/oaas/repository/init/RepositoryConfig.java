package org.hpcclab.oaas.repository.init;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(
  prefix = "oaas.repo",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface RepositoryConfig {
  @WithDefault("false")
  boolean createOnStart();
  ObjectCache object();
  StateCache state();
  CompletionCache completion();

  interface ObjectCache{
    @WithDefault("true")
    boolean persist();
    @WithDefault("256MB")
    String maxSize();
    @WithDefault("2000")
    int nearCacheMaxEntry();
  }
  interface StateCache{
    @WithDefault("true")
    boolean persist();
    @WithDefault("128MB")
    String maxSize();
    @WithDefault("-1")
    int nearCacheMaxEntry();
  }
  interface CompletionCache{
    @WithDefault("true")
    boolean persist();
    @WithDefault("128MB")
    String maxSize();
    @WithDefault("-1")
    int nearCacheMaxEntry();
  }
}
