package org.hpcclab.oaas.crm;

import java.util.Map;

public record CrtMappingConfig(
  Map<String, CrtConfig> templates
) {
  public record CrtConfig(
    String type,
    Map<String, String> images,
    Map<String, Map<String, String>> additionalEnv,
    String optimizer,
    Map<String, String> optimizerConf
  ){}
}
