package org.hpcclab.oaas.crm.controller;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.serving.v1.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.proto.ProtoDeploymentCondition;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOFunctionDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.crm.controller.K8SCrController.*;
import static org.hpcclab.oaas.crm.controller.K8sResourceUtil.makeAnnotation;
import static org.hpcclab.oaas.crm.controller.K8sResourceUtil.makeResourceRequirements;

/**
 * @author Pawissanutt
 */
public class KnativeFnCrComponentController extends AbstractK8sCrComponentController
  implements FnCrComponentController<HasMetadata> {
  private static final Logger logger = LoggerFactory.getLogger(KnativeFnCrComponentController.class);
  final CrtMappingConfig.FnConfig fnConfig;
  final ProtoOFunction function;
  KnativeClient knativeClient;

  protected KnativeFnCrComponentController(CrtMappingConfig.FnConfig fnConfig,
                                           OprcEnvironment.Config envConfig,
                                           ProtoOFunction function) {
    super(null, envConfig);
    this.fnConfig = fnConfig;
    this.function = function;
  }

  @Override
  public void init(CrController parentController) {
    super.init(parentController);
    if (parentController instanceof K8SCrController k8SCrController) {
      this.kubernetesClient = k8SCrController.kubernetesClient;
      this.knativeClient = new DefaultKnativeClient(kubernetesClient);
    }
  }

  @Override
  protected List<HasMetadata> doCreateDeployOperation(CrDeploymentPlan plan) {
    logger.debug("deploy function {} with Knative", function.getKey());
    var instanceSpec = plan.fnInstances()
      .get(function.getKey());
    var knConf = function.getProvision()
      .getKnative().toBuilder();
    var labels = Maps.mutable.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_COMPONENT_LABEL_KEY, NAME_FUNCTION,
      CR_FN_KEY, function.getKey()
    );

    if (!envConfig.exposeKnative()) {
      labels.put("networking.knative.dev/visibility", "cluster-local");
    }
    knConf.getImage();
    if (knConf.getImage().isEmpty())
      return List.of();
    var containerBuilder = new ContainerBuilder()
      .withName("fn")
      .withImage(knConf.getImage())
      .addAllToEnv(K8sResourceUtil.extractEnv(function))
      .withResources(makeResourceRequirements(instanceSpec));

    if (knConf.getPort() > 0) {
      ContainerPortBuilder port = new ContainerPortBuilder()
        .withProtocol("TCP")
        .withContainerPort(knConf.getPort());
      logger.debug("knconf {}", knConf);
      if (function.getConfig().getHttp2())
        port = port.withName("h2c");
      containerBuilder.withPorts(port.build()
      );
    }

    var fnName = createName(function.getKey());
    var annotation = makeAnnotation(Maps.mutable.empty(), knConf, fnConfig);
    makeAnnotation(annotation, instanceSpec);
    var serviceBuilder = new ServiceBuilder()
      .withNewMetadata()
      .withName(fnName)
      .withLabels(labels)
      .endMetadata();
    serviceBuilder.withNewSpec()
      .withNewTemplate()
      .withNewMetadata()
      .withAnnotations(annotation)
      .addToLabels(labels)
      .endMetadata()
      .withNewSpec()
      .withTimeoutSeconds(600L)
      .withContainerConcurrency(knConf.getConcurrency() > 0 ?
        (long) knConf.getConcurrency():null)
      .withContainers(containerBuilder.build())
      .endSpec()
      .endTemplate()
      .endSpec();
    return List.of(serviceBuilder.build());
  }


  @Override
  protected List<HasMetadata> doCreateAdjustOperation(CrAdjustmentPlan plan) {
    CrInstanceSpec spec = plan.fnInstances().get(function.getKey());
    var svc = knativeClient.services()
      .inNamespace(namespace)
      .withName(createName(function.getKey()))
      .get();
    if (svc==null) return List.of();

    makeAnnotation(svc.getSpec()
      .getTemplate()
      .getMetadata()
      .getAnnotations(), spec);
    return List.of(svc);
  }

  @Override
  public List<HasMetadata> doCreateDeleteOperation() {
    List<HasMetadata> resources = Lists.mutable.empty();
    var labels = Map.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_FN_KEY, function.getKey()
    );
    var services = knativeClient.services()
      .withLabels(labels)
      .list()
      .getItems();
    resources.addAll(services);
    return resources;
  }

  private String createName(String key) {
    return prefix + "fn-" + key
      .replaceAll("[._]", "-");
  }

  @Override
  public OFunctionStatusUpdate buildStatusUpdate() {
    var statusBuilder = ProtoOFunctionDeploymentStatus.newBuilder()
      .setCondition(ProtoDeploymentCondition.PROTO_DEPLOYMENT_CONDITION_DEPLOYING);
    if (!function.getStatus().getInvocationUrl().isEmpty()) {
      statusBuilder.setInvocationUrl(function.getStatus().getInvocationUrl());
      statusBuilder.setCondition(ProtoDeploymentCondition.PROTO_DEPLOYMENT_CONDITION_RUNNING);
    }
    return OFunctionStatusUpdate.newBuilder()
      .setKey(function.getKey())
      .setStatus(statusBuilder
        .build())
      .setProvision(function.getProvision())
      .build();
  }
}
