package org.hpcclab.oaas.crm;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection(ignoreNested=false)
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
