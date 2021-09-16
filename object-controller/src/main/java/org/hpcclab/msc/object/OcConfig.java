package org.hpcclab.msc.object;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "oaas.oc", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OcConfig {
  String s3PrefixUrl();
}
