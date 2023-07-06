package org.hpcclab.oaas.sa;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.hpcclab.oaas.storage.S3ConnConf;

@ConfigMapping(prefix = "oaas.sa", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface SaConfig {

  S3ConnConf s3();
  @WithDefault("false")
  boolean relay();
}
