package org.hpcclab.oaas.orbit.env;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.orbit.OrbitManagerConfig;

@ApplicationScoped
public class EnvironmentManager {
  OprcEnvironment environment;

  @Inject
  public EnvironmentManager(OrbitManagerConfig conf) {
    var kafka = ConfigProvider.getConfig()
      .getValue("oprc.envconf.kafka", String.class);
    var clsManagerHost = ConfigProvider.getConfig()
      .getValue("oprc.envconf.clsManagerHost", String.class);
    var clsManagerPort = ConfigProvider.getConfig()
      .getValue("oprc.envconf.clsManagerPort", String.class);
    environment = new OprcEnvironment(
      new OprcEnvironment.Config(
        kafka,
        clsManagerHost,
        clsManagerPort),
      null,
      null
    );
  }

  public OprcEnvironment getEnvironment() {
    return environment;
  }
}
