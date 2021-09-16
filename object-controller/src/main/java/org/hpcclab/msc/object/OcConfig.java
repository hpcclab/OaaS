package org.hpcclab.msc.object;

import io.smallrye.config.ConfigMapping;

import java.net.URL;

@ConfigMapping(prefix = "oaas.oc", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OcConfig {
  URL s3PrefixUrl();
}
