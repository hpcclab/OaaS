package org.hpcclab.oaas.crm.env;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetrics;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.crm.CrmConfig;
import org.hpcclab.oaas.crm.env.OprcEnvironment.EnvResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EnvironmentManager {
  private static final Logger logger = LoggerFactory.getLogger(EnvironmentManager.class);
  final KubernetesClient client;
  OprcEnvironment environment;
  OprcEnvironment.Config envConf;

  @Inject
  public EnvironmentManager(KubernetesClient client,
                            CrmConfig conf) {
    this.client = client;
    var kafka = ConfigProvider.getConfig()
      .getValue("oprc.envconf.kafka", String.class);
    var pmHost = ConfigProvider.getConfig()
      .getValue("oprc.envconf.pmHost", String.class);
    var pmPort = ConfigProvider.getConfig()
      .getValue("oprc.envconf.pmPort", String.class);
    envConf = new OprcEnvironment.Config(
      kafka,
      pmHost,
      pmPort,
      conf.exposeKnative()
    );
    environment = new OprcEnvironment(
      envConf,
      null,
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
    EnvResource total = nodes.stream()
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
    EnvResource requests = calculateRequest();
    EnvResource remaining = total.subtract(requests);
    logger.info("current resources: total {}, usage {}, remaining {}", total, usage, remaining);
    environment = new OprcEnvironment(
      envConf,
      total,
      remaining,
      requests
    );
  }

  public EnvResource calculateRequest() {
    PodList podList = client.pods().inAnyNamespace().list();
    // Initialize a map to store total requested resources by node
    double totalCPURequests = 0.0;
    long totalMemoryRequests = 0L;

    // Iterate through each pod and accumulate resource requests by node
    for (Pod pod : podList.getItems()) {
      for (Container container : pod.getSpec().getContainers()) {
        if (container.getResources() != null && container.getResources().getRequests() != null) {
          totalCPURequests += container.getResources().getRequests().get("cpu").getNumericalAmount().doubleValue();
          totalMemoryRequests += container.getResources().getRequests().get("memory").getNumericalAmount().longValue();

        }
      }
    }
    return new EnvResource(totalCPURequests, totalMemoryRequests);
  }



}
