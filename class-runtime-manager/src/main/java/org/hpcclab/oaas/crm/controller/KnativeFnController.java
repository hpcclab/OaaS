package org.hpcclab.oaas.crm.controller;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.serving.v1.RevisionSpec;
import io.fabric8.knative.serving.v1.RevisionTemplateSpec;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceBuilder;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.exception.CrUpdateException;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.proto.ProtoDeploymentCondition;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOFunctionDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.crm.CrmConfig.LABEL_KEY;
import static org.hpcclab.oaas.crm.controller.K8SCrController.*;

@RegisterForReflection(
  targets = {
    Service.class,
    io.fabric8.knative.serving.v1.ServiceSpec.class,
    RevisionTemplateSpec.class,
    RevisionSpec.class,
    ServiceStatus.class
  }
)
public class KnativeFnController implements FnController {
  private static final Logger logger = LoggerFactory.getLogger(KnativeFnController.class);
  KubernetesClient kubernetesClient;
  KnativeClient knativeClient;
  K8SCrController controller;
  OprcEnvironment.Config envConfig;

  public KnativeFnController(KubernetesClient kubernetesClient,
                             K8SCrController controller,
                             OprcEnvironment.Config envConfig) {
    this.kubernetesClient = kubernetesClient;
    this.knativeClient = new DefaultKnativeClient(kubernetesClient);
    this.controller = controller;
    this.envConfig = envConfig;
  }

  @Override
  public FnResourcePlan deployFunction(CrDeploymentPlan plan, ProtoOFunction function)
    throws CrDeployException {
    var instance = plan.fnInstances()
      .getOrDefault(function.getKey(), 0);
    var knConf = function.getProvision()
      .getKnative();
    var labels = Maps.mutable.of(
      CR_LABEL_KEY, String.valueOf(controller.id),
      CR_COMPONENT_LABEL_KEY, "function",
      CR_FN_KEY, function.getKey()
    );

    if (!envConfig.exposeKnative()) {
      labels.put("networking.knative.dev/visibility", "cluster-local");
    }
    knConf.getImage();
    if (knConf.getImage().isEmpty())
      return FnResourcePlan.EMPTY;
    Map<String, Quantity> requests = new HashMap<>();
    if (!knConf.getRequestsCpu().isEmpty()) {
      requests.put("cpu", Quantity.parse(knConf.getRequestsCpu()));
    }
    if (!knConf.getRequestsMemory().isEmpty()) {
      requests.put("memory", Quantity.parse(knConf.getRequestsMemory()));
    }
    Map<String, Quantity> limits = new HashMap<>();
    if (!knConf.getLimitsCpu().isEmpty()) {
      limits.put("cpu", Quantity.parse(knConf.getLimitsCpu()));
    }
    if (!knConf.getLimitsMemory().isEmpty()) {
      limits.put("memory", Quantity.parse(knConf.getLimitsMemory()));
    }
    var containerBuilder = new ContainerBuilder()
      .withName("fn")
      .withImage(knConf.getImage())
      .addAllToEnv(knConf.getEnvMap()
        .entrySet().stream().map(e -> new EnvVar(e.getKey(), e.getValue(), null))
        .toList()
      )
      .withResources(new ResourceRequirementsBuilder()
        .withRequests(requests)
        .withLimits(limits)
        .build()
      );

    if (knConf.getPort() > 0) {
      containerBuilder.withPorts(new ContainerPortBuilder()
        .withProtocol("TCP")
        .withContainerPort(knConf.getPort() <= 0 ? 8080:knConf.getPort())
        .build()
      );
    }

    var fnName = controller.prefix + function.getKey().toLowerCase().replaceAll("[\\._]", "-");
    var annotation = new HashMap<String, String>();
    if (instance >= 0)
      annotation.put("autoscaling.knative.dev/minScale",
        String.valueOf(instance));
    if (knConf.getMaxScale() >= 0)
      annotation.put("autoscaling.knative.dev/maxScale",
        String.valueOf(knConf.getMaxScale()));
    if (!knConf.getScaleDownDelay().isEmpty())
      annotation.put("autoscaling.knative.dev/scale-down-delay",
        knConf.getScaleDownDelay());
    if (knConf.getTargetConcurrency() > 0)
      annotation.put("autoscaling.knative.dev/target",
        String.valueOf(knConf.getTargetConcurrency()));
    var serviceBuilder = new ServiceBuilder()
      .withNewMetadata()
      .withName(fnName)
      .withLabels(labels)
      .endMetadata();
    serviceBuilder.withNewSpec()
      .withNewTemplate()
      .withNewMetadata()
      .withAnnotations(annotation)
      .addToLabels(LABEL_KEY, function.getKey())
      .endMetadata()
      .withNewSpec()
      .withTimeoutSeconds(600L)
      .withContainerConcurrency(knConf.getConcurrency() > 0 ?
        (long) knConf.getConcurrency():null)
      .withContainers(containerBuilder.build())
      .endSpec()
      .endTemplate()
      .endSpec()
    ;
    return new FnResourcePlan(
      List.of(serviceBuilder.build()),
      List.of(OFunctionStatusUpdate.newBuilder()
        .setKey(function.getKey())
        .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
          .setCondition(ProtoDeploymentCondition.PROTO_DEPLOYMENT_CONDITION_DEPLOYING)
          .build())
        .build())
    );
  }

  @Override
  public List<HasMetadata> removeFunction(String fnKey) throws CrUpdateException {
    List<HasMetadata> resources = Lists.mutable.empty();
    var labels = Map.of(
      CR_LABEL_KEY, String.valueOf(controller.id),
      CR_FN_KEY, fnKey
    );
    var services = knativeClient.services()
      .withLabels(labels)
      .list()
      .getItems();
    resources.addAll(services);
    return resources;
  }

  @Override
  public List<HasMetadata> removeAllFunction() {
    List<HasMetadata> resources = Lists.mutable.empty();
    var labels = Map.of(
      CR_LABEL_KEY, String.valueOf(controller.id)
    );
    var services = knativeClient.services()
      .withLabels(labels)
      .list()
      .getItems();
    resources.addAll(services);
    logger.info("remove ksvc [{}]", resources.size());
    return resources;
  }
}
