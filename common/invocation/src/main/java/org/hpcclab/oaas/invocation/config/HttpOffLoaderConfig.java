package org.hpcclab.oaas.invocation.config;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder()
public class HttpOffLoaderConfig {
  @Builder.Default
  String appName = "oparaca";
  @Builder.Default
  String ceType = "oaas.task";
  @Builder.Default
  int timout = 10*60*1000;
  @Builder.Default
  boolean enabledCeHeader = true;
  @Builder.Default
  int connectionPoolMaxSize = 100;
  @Builder.Default
  int h2ConnectionPoolMaxSize = 3;
  @Builder.Default
  int connectTimeout = 1000;
}
