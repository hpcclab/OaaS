package org.hpcclab.oaas.orbit.env;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.orbit.OrbitManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EnvironmentManager {
  private static final Logger logger = LoggerFactory.getLogger(EnvironmentManager.class);
  final KubernetesClient client;
  OprcEnvironment environment;
  OprcEnvironment.Config envConf;

  @Inject
  public EnvironmentManager(KubernetesClient client,
                            OrbitManagerConfig conf) {
    this.client = client;
    var kafka = ConfigProvider.getConfig()
      .getValue("oprc.envconf.kafka", String.class);
    var clsManagerHost = ConfigProvider.getConfig()
      .getValue("oprc.envconf.clsManagerHost", String.class);
    var clsManagerPort = ConfigProvider.getConfig()
      .getValue("oprc.envconf.clsManagerPort", String.class);
    envConf = new OprcEnvironment.Config(
      kafka,
      clsManagerHost,
      clsManagerPort);
    environment = new OprcEnvironment(
      envConf,
      null,
      null
    );
  }

  public OprcEnvironment getEnvironment() {
    refresh();
    return environment;
  }

  public void refresh() {
    var metrics = client.top().nodes()
      .metric();
    logger.info("metrics {}", metrics);
  }
}
