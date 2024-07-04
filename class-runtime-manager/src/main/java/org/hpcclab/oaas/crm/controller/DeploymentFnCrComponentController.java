package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import org.eclipse.collections.api.factory.Lists;
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
import static org.hpcclab.oaas.crm.controller.K8sResourceUtil.makeResourceRequirements;

/**
 * @author Pawissanutt
 */
public class DeploymentFnCrComponentController extends AbstractK8sCrComponentController
  implements FnCrComponentController<HasMetadata> {
  private static final Logger logger = LoggerFactory.getLogger(DeploymentFnCrComponentController.class);
  final CrtMappingConfig.FnConfig fnConfig;
  final ProtoOFunction function;
  final boolean enableHpa;

  protected DeploymentFnCrComponentController(CrtMappingConfig.FnConfig fnConfig,
                                              OprcEnvironment.Config envConfig,
                                              ProtoOFunction function) {
    super(null, envConfig);
    this.fnConfig = fnConfig;
    this.function = function;
    this.enableHpa = fnConfig.enableHpa();
    logger.debug("HPA is enabled ({}) for {}", enableHpa, function.getKey());
  }

  @Override
  public void init(CrController parentController) {
    super.init(parentController);
    if (parentController instanceof K8SCrController k8SCrController) {
      this.kubernetesClient = k8SCrController.kubernetesClient;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  protected List<HasMetadata> doCreateDeployOperation(CrDeploymentPlan plan) {
    logger.debug("deploy function {} with Deployment", function.getKey());
    var instanceSpec = plan.fnInstances()
      .get(function.getKey());
    var labels = Map.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_COMPONENT_LABEL_KEY, NAME_FUNCTION,
      CR_FN_KEY, function.getKey()
    );
    var deployConf = function.getProvision()
      .getDeployment();
    deployConf.getImage();
    if (deployConf.getImage().isEmpty())
      return List.of();

    List<HasMetadata> resources = Lists.mutable.of();
    var container = new ContainerBuilder()
      .withName("fn")
      .withImage(deployConf.getImage())
      .addAllToEnv(K8sResourceUtil.extractEnv(function))
      .withPorts(new ContainerPortBuilder()
        .withName("http")
        .withProtocol("TCP")
        .withContainerPort(deployConf.getPort() <= 0 ? 8080:deployConf.getPort())
        .build()
      )
      .withImagePullPolicy(deployConf.getPullPolicy().isEmpty() ? null:deployConf.getPullPolicy())
      .withResources(makeResourceRequirements(instanceSpec))
      .build();
    var fnName = createName(function.getKey());
    var deploymentBuilder = new DeploymentBuilder()
      .withNewMetadata()
      .withName(fnName)
      .withLabels(labels)
      .endMetadata();
    deploymentBuilder
      .withNewSpec()
      .withReplicas(instanceSpec.minInstance() > 0 ? instanceSpec.minInstance():1)
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
    resources.add(deployment);
    resources.add(svc);
    if (enableHpa) {
      var hpa = createHpa(instanceSpec, labels, fnName, fnName);
      resources.add(hpa);
    }
    return resources;
  }


  @Override
  protected List<HasMetadata> doCreateAdjustOperation(CrAdjustmentPlan plan) {
    CrInstanceSpec spec = plan.fnInstances().get(function.getKey());
    String deployName = createName(function.getKey());
    if (enableHpa) {
      HorizontalPodAutoscaler hpa = editHpa(spec, deployName);
      return hpa==null ? List.of():List.of(hpa);
    } else {
      var deployment = kubernetesClient.apps()
        .deployments()
        .inNamespace(namespace)
        .withName(deployName)
        .get();
      deployment.getSpec()
        .setReplicas(spec.minInstance());
      return List.of(deployment);
    }
  }

  @Override
  public List<HasMetadata> doCreateDeleteOperation() {
    List<HasMetadata> resources = Lists.mutable.empty();
    var labels = Map.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_FN_KEY, function.getKey()
    );
    var fnDeployment = createName(function.getKey());
    var deployments = kubernetesClient.apps()
      .deployments()
      .withName(fnDeployment)
      .get();
    if (deployments != null)
      resources.add(deployments);
    var services = kubernetesClient.services()
      .withName(fnDeployment)
      .get();
    if (services != null)
      resources.add(services);
    if (enableHpa) {
      var hpa = kubernetesClient.autoscaling().v2().horizontalPodAutoscalers()
        .withName(fnDeployment).get();
      if (hpa != null)
        resources.add(hpa);
    }
    return resources;
  }

  private String createName(String key) {
    return prefix + "fn-" + key
      .replaceAll("[._]", "-") + "-00001";
  }

  @Override
  public OFunctionStatusUpdate buildStatusUpdate() {
    var statusBuilder = ProtoOFunctionDeploymentStatus.newBuilder()
      .setInvocationUrl("http://" + createName(function.getKey()) + "." + namespace + ".svc.cluster.local")
      .setCondition(ProtoDeploymentCondition.PROTO_DEPLOYMENT_CONDITION_RUNNING)
      .setTs(System.currentTimeMillis());
    return OFunctionStatusUpdate.newBuilder()
      .setKey(function.getKey())
      .setStatus(statusBuilder
        .build())
      .setProvision(function.getProvision())
      .build();
  }
}
