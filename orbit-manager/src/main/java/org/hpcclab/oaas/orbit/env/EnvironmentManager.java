package org.hpcclab.oaas.orbit.env;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetrics;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.orbit.OrbitManagerConfig;
import org.hpcclab.oaas.orbit.env.OprcEnvironment.EnvResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

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
    var nodeMetricsList = client.top().nodes()
      .metrics().getItems();
    List<Node> nodes = client.nodes().list().getItems();
    EnvResource total  = nodes.stream()
      .map(node -> node.getStatus().getAllocatable())
      .map(m -> new EnvResource(
        m.get("cpu").getNumericalAmount(),
        m.get("memory").getNumericalAmount())
      )
      .reduce(EnvResource.ZERO, EnvResource::sum);
    EnvResource usage = nodeMetricsList.stream()
      .map(NodeMetrics::getUsage)
      .map(EnvResource::new)
      .reduce(EnvResource.ZERO, EnvResource::sum);
    EnvResource remaining = total.subtract(usage);
    logger.info("current resources: total {}, usage {}, remaining {}", total, usage, remaining);
    environment = new OprcEnvironment(
      envConf,
      total,
      remaining
    );
  }


}
