package org.hpcclab.oaas.orbit;

import java.util.Map;

public record OrbitMappingConfig(
  Map<String, OrbitTemplateConfig> templates
) {
  public record OrbitTemplateConfig(
    Map<String, String> images
  ){}
}
