package org.hpcclab.oaas.invocation;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Builder()
public class HttpInvokerConfig {
  String appName;
  @Builder.Default
  String ceType = "oaas.task";
}
