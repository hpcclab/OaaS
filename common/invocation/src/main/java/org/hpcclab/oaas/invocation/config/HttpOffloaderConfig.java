package org.hpcclab.oaas.invocation.config;

import lombok.*;

@Getter
@Builder()
public class HttpOffloaderConfig {
  String appName;
  @Builder.Default
  String ceType = "oaas.task";
  int timout = 10*60*1000;
}
