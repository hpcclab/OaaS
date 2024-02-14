package org.hpcclab.oaas.crm;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "oprc.crm", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface CrmConfig {
  boolean exposeKnative();
  String pmHost();
  int pmPort();
  String promUrl();

  Optional<String> templateOverride();
  String LABEL_KEY = "oaas.function";
}
