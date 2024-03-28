package org.hpcclab.oprc.cli;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * @author Pawissanutt
 */
@ConfigMapping(
  prefix = "oprc.cli",
  namingStrategy = ConfigMapping.NamingStrategy.VERBATIM
)
public interface CliConfig {
  @WithDefault(".oprc/config.yml")
  String configPath();
}
