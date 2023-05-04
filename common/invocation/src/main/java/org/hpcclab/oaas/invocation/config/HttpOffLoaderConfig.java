package org.hpcclab.oaas.invocation.config;

import lombok.*;

@Getter
@Builder()
public class HttpOffLoaderConfig {
  String appName;
  @Builder.Default
  String ceType = "oaas.task";
  @Builder.Default
  int timout = 10*60*1000;
}
