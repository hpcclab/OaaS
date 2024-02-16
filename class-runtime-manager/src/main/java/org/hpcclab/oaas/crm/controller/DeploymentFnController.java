package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.proto.ProtoDeploymentCondition;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOFunctionDeploymentStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.crm.controller.K8SCrController.*;

public class DeploymentFnController implements FnController {
  KubernetesClient kubernetesClient;
  K8SCrController controller;

  public DeploymentFnController(KubernetesClient kubernetesClient, K8SCrController controller) {
    this.kubernetesClient = kubernetesClient;
    this.controller = controller;
  }

  @Override
  public FnResourcePlan deployFunction(CrDeploymentPlan plan,
                                       ProtoOFunction function) {

    var instance = plan.fnInstances()
      .getOrDefault(function.getKey(), 0);
    var labels = Map.of(
      CR_LABEL_KEY, String.valueOf(controller.id),
      CR_COMPONENT_LABEL_KEY, "function",
      CR_FN_KEY, function.getKey()
    );
    var deployConf = function.getProvision()
      .getDeployment();
    deployConf.getImage();
    if (deployConf.getImage().isEmpty())
      return FnResourcePlan.EMPTY;
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
    var fnName = createName(function.getKey());
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
    return new FnResourcePlan(List.of(deployment, svc),
      List.of(OFunctionStatusUpdate.newBuilder()
        .setKey(function.getKey())
        .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
          .setCondition(ProtoDeploymentCondition.PROTO_DEPLOYMENT_CONDITION_RUNNING)
          .setInvocationUrl("http://" + svc.getMetadata().getName() + "." + controller.namespace + ".svc.cluster.local")
          .build())
        .build())
    );
  }

  @Override
  public FnResourcePlan applyAdjustment(CrAdjustmentPlan plan) {
    List<HasMetadata> resource = Lists.mutable.empty();
    for (Map.Entry<String, Integer> entry : plan.fnInstances().entrySet()) {
      var fnKey = entry.getKey();
      var deployment = kubernetesClient.apps()
        .deployments()
        .inNamespace(controller.namespace)
        .withName(createName(fnKey))
        .get();
      if (deployment==null) continue;
      deployment.getSpec()
        .setReplicas(entry.getValue());
      resource.add(deployment);
    }
    return new FnResourcePlan(
      resource,
      List.of()
    );
  }

  private String createName(String fnKey) {
    return controller.prefix + "fn-" + fnKey.toLowerCase().replaceAll("[\\._]", "-");

  }

  @Override
  public List<HasMetadata> removeFunction(String fnKey) {
    List<HasMetadata> resources = Lists.mutable.empty();
    var labels = Map.of(
      CR_LABEL_KEY, String.valueOf(controller.id),
      CR_FN_KEY, fnKey
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

  @Override
  public List<HasMetadata> removeAllFunction() {
    return List.of();
  }
}
