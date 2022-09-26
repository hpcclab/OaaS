package org.hpcclab.oaas.invocation;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class InvocationConfig {
  String storageAdapterUrl;
}
