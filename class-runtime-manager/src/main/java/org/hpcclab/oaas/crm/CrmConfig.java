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

  String LABEL_KEY = "oaas.functions";

  @WithDefault("0.99")
  double uptimePercentage();

  @WithDefault("oaas-fn")
  String fnProvisionTopic();

  @WithDefault("oaas-cls")
  String clsProvisionTopic();

  @WithDefault("oaas-cr-hash")
  String crHashTopic();
}
