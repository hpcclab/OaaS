package org.hpcclab.oaas.sa;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.hpcclab.oaas.storage.S3ConnConf;

@ConfigMapping(prefix = "oprc.sa", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface SaConfig {
  @WithDefault("false")
  boolean relay();
}
