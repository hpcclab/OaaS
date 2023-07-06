package org.hpcclab.oaas.storage;

import io.smallrye.config.WithDefault;

import java.net.URI;
import java.util.Optional;

public interface S3ConnConf {
  @WithDefault("accessKey")
  String accessKey();
  @WithDefault("secretKey")
  String secretKey();
  @WithDefault("http://localhost:9000")
  URI url();
  @WithDefault("http://localhost:9000")
  URI publicUrl();
  @WithDefault("true")
  boolean pathStyle();
  @WithDefault("oprc")
  String bucket();

  Optional<String> prefix();
  @WithDefault("us-east-1")
  String region();
}
