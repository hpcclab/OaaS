package org.hpcclab.oaas.crm;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "oprc.crm", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface CrmConfig {
  boolean exposeKnative();
  String pmHost();
  int pmPort();
  String promUrl();
  Optional<String> templateOverride();
  @WithDefault("10000")
  int stabilizationWindow();
  String LABEL_KEY = "oaas.function";
}
