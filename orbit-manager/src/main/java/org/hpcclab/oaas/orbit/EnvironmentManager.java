package org.hpcclab.oaas.orbit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class EnvironmentManager {
  OprcEnvironment environment;

  @Inject
  public EnvironmentManager() {
    var kafka = ConfigProvider.getConfig().getOptionalValue("oprc.envconf.kafka", String.class);
    environment = new OprcEnvironment(new OprcEnvironment.Config(kafka.orElseThrow()));
  }

  public OprcEnvironment getEnvironment() {
    return environment;
  }
}
