package org.hpcclab.oaas.invocation.config;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Builder()
public class InvocationConfig {
  String storageAdapterUrl;
}
