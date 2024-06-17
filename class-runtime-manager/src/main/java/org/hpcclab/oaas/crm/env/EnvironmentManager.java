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

import java.util.List;

@ApplicationScoped
public class EnvironmentManager {
  private static final Logger logger = LoggerFactory.getLogger(EnvironmentManager.class);
  final KubernetesClient client;
  OprcEnvironment environment;

  @Inject
  public EnvironmentManager(KubernetesClient client,
                            CrmConfig conf) {
    this.client = client;
    var configProvider = ConfigProvider.getConfig();
    var kafka = configProvider
      .getValue("oprc.envconf.kafka", String.class);
    var pmHost = configProvider
      .getValue("oprc.envconf.pmHost", String.class);
    var pmPort = configProvider
      .getValue("oprc.envconf.pmPort", String.class);
    var envConf = OprcEnvironment.Config.builder()
      .namespace(conf.namespace())
      .kafkaBootstrap(kafka)
      .classManagerHost(pmHost)
      .classManagerPort(pmPort)
      .exposeKnative(conf.exposeKnative())
      .useKnativeLb(conf.useKnativeLb())
      .logLevel(configProvider.getValue("oprc.log", String.class))
      .clsTopic(conf.clsProvisionTopic())
      .fnTopic(conf.fnProvisionTopic())
      .crHashTopic(conf.crHashTopic())
      .build();
    environment = OprcEnvironment.builder()
      .config(envConf)
      .availability(
        new OprcEnvironment.AvailabilityInfo(conf.uptimePercentage())
      )
      .build();
  }

  public OprcEnvironment getEnvironment() {
    refresh();
    return environment;
  }

  public OprcEnvironment.Config getEnvironmentConfig() {
    return environment.config();
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
    logger.info("current resources: total {}, usage {}, remaining {}",
      total, usage, remaining);
    environment = environment.toBuilder()
      .total(total)
      .usable(remaining)
      .request(requests)
      .build();
  }

  public EnvResource calculateRequest() {
    PodList podList = client.pods().inAnyNamespace().list();
    // Initialize a map to store total requested resources by node
    double totalCPURequests = 0.0;
    long totalMemoryRequests = 0L;

    // Iterate through each pod and accumulate resource requests by node
    for (Pod pod : podList.getItems()) {
      for (Container container : pod.getSpec().getContainers()) {
        if (container.getResources()==null ||
          container.getResources().getRequests()==null) continue;
        var cpu = container.getResources().getRequests().get("cpu");
        var mem = container.getResources().getRequests().get("memory");
        if (cpu!=null)
          totalCPURequests += cpu.getNumericalAmount().doubleValue();
        if (mem!=null)
          totalMemoryRequests += mem.getNumericalAmount().longValue();
      }
    }
    return new EnvResource(totalCPURequests, totalMemoryRequests);
  }
}
