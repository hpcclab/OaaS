package org.hpcclab.oaas.crm;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "oprc.crm", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface CrmConfig {
  boolean exposeKnative();
  String pmHost();
  int pmPort();
  String LABEL_KEY = "oaas.function";
}
