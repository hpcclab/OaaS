package org.hpcclab.oaas.orbit.controller;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.orbit.OrbitTemplate;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.exception.OrbitDeployException;
import org.hpcclab.oaas.orbit.exception.OrbitUpdateException;
import org.hpcclab.oaas.orbit.optimize.OrbitDeploymentPlan;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOrbit;
import org.hpcclab.oaas.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentOrbitController extends BaseK8sOrbitController {
  private static final Logger logger = LoggerFactory.getLogger(DeploymentOrbitController.class);

  public DeploymentOrbitController(OrbitTemplate template,
                                   KubernetesClient client,
                                   OprcEnvironment.Config envConfig,
                                   Tsid tsid) {
    super(template, client, envConfig, tsid);
  }

  public DeploymentOrbitController(OrbitTemplate template,
                                   KubernetesClient client,
                                   OprcEnvironment.Config envConfig,
                                   ProtoOrbit orbit) {
    this(template, client, envConfig, Tsid.from(orbit.getId()));
    attachedCls.addAll(orbit.getAttachedClsList());
    attachedFn.addAll(orbit.getAttachedFnList());
  }

  public List<HasMetadata> deployFunction(OrbitDeploymentPlan plan,
                                          ProtoOFunction function) throws OrbitDeployException {

    var instance = plan.fnInstances()
      .getOrDefault(function.getKey(), 0);
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id),
      ORBIT_COMPONENT_LABEL_KEY, "function",
      ORBIT_FN_KEY, function.getKey()
    );
    var deployConf = function.getProvision()
      .getDeployment();
    deployConf.getImage();
    if (deployConf.getImage().isEmpty())
      return List.of();
    Map<String, Quantity> requests = new HashMap<>();
    if (!deployConf.getRequestsCpu().isEmpty()) {
      requests.put("cpu", Quantity.parse(deployConf.getRequestsCpu()));
    }
    if (!deployConf.getRequestsMemory().isEmpty()) {
      requests.put("memory", Quantity.parse(deployConf.getRequestsMemory()));
    }
    Map<String, Quantity> limits = new HashMap<>();
    if (!deployConf.getLimitsCpu().isEmpty()) {
      limits.put("cpu", Quantity.parse(deployConf.getLimitsCpu()));
    }
    if (!deployConf.getLimitsMemory().isEmpty()) {
      limits.put("memory", Quantity.parse(deployConf.getLimitsMemory()));
    }
    var container = new ContainerBuilder()
      .withName("fn")
      .withImage(deployConf.getImage())
      .addAllToEnv(deployConf.getEnvMap()
        .entrySet().stream().map(e -> new EnvVar(e.getKey(), e.getValue(), null))
        .toList()
      )
      .withPorts(new ContainerPortBuilder()
        .withName("http")
        .withProtocol("TCP")
        .withContainerPort(deployConf.getPort() <= 0 ? 8080:deployConf.getPort())
        .build()
      )
      .withResources(new ResourceRequirementsBuilder()
        .withRequests(requests)
        .withLimits(limits)
        .build()
      )
      .build();
    var fnName = prefix + function.getKey().toLowerCase().replaceAll("[\\._]", "-");
    var deploymentBuilder = new DeploymentBuilder()
      .withNewMetadata()
      .withName(fnName)
      .withLabels(labels)
      .endMetadata();
    deploymentBuilder
      .withNewSpec()
      .withReplicas(instance)
      .withNewSelector()
      .addToMatchLabels(labels)
      .endSelector()
      .withNewTemplate()
      .withNewMetadata()
      .addToLabels(labels)
      .endMetadata()
      .withNewSpec()
      .addToContainers(container)
      .endSpec()
      .endTemplate()
      .endSpec();
    var deployment = deploymentBuilder.build();
    var svc = new ServiceBuilder()
      .withNewMetadata()
      .withName(fnName)
      .withLabels(labels)
      .endMetadata()
      .withNewSpec()
      .addToSelector(labels)
      .addToPorts(
        new ServicePortBuilder()
          .withName("http")
          .withProtocol("TCP")
          .withPort(80)
          .withTargetPort(new IntOrString(deployConf.getPort() <= 0 ? 8080:deployConf.getPort()))
          .build()
      )
      .endSpec()
      .build();
    return List.of(deployment, svc);
  }

  public List<HasMetadata> removeFunction(String fnKey) throws OrbitUpdateException {
    List<HasMetadata> resources = Lists.mutable.empty();
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id),
      ORBIT_FN_KEY, fnKey
    );
    var deployments = kubernetesClient.apps()
      .deployments()
      .withLabels(labels)
      .list()
      .getItems();
    resources.addAll(deployments);
    var services = kubernetesClient.services().withLabels(labels)
      .list().getItems();
    resources.addAll(services);
    return resources;
  }
}
