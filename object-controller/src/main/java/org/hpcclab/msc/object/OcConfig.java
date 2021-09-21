package org.hpcclab.msc.object;

import io.smallrye.config.ConfigMapping;

import java.net.URL;
import java.nio.file.Path;

@ConfigMapping(prefix = "oaas.oc", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface OcConfig {
  String s3PrefixUrl();
}
