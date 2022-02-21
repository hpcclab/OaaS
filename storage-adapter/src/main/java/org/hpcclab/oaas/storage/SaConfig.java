package org.hpcclab.oaas.storage;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ConfigMapping(prefix = "oaas.sa", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface SaConfig {

  S3Config s3();

  interface S3Config{
    String accessKey();
    String secretKey();
    String url();
    String publicUrl();
    @WithDefault("us-east-1")
    String region();
    @WithDefault("oaas-bkt")
    String bucket();
  }
}
