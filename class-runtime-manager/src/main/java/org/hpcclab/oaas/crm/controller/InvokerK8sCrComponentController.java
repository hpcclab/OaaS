package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.crm.OprcComponent.INVOKER;
import static org.hpcclab.oaas.crm.controller.K8SCrController.*;

/**
 * @author Pawissanutt
 */
public class InvokerK8sCrComponentController extends AbstractK8sCrComponentController {

  public InvokerK8sCrComponentController(CrtMappingConfig.SvcConfig svcConfig) {
    super(svcConfig);
  }

  @Override
  public List<HasMetadata> createDeployOperation(CrInstanceSpec instanceSpec) {
    var labels = Map.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_COMPONENT_LABEL_KEY, INVOKER.getSvc()
    );
    var deployment = createDeployment(
      "/crts/invoker-dep.yml",
      prefix + INVOKER.getSvc(),
      labels,
      instanceSpec
    );
    var podMonitor = K8sResourceUtil
      .createPodMonitor(prefix + INVOKER.getSvc(), namespace, labels);
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var invokerSvc = createSvc(
      "/crts/invoker-svc.yml",
      prefix + INVOKER.getSvc(),
      labels);
    var invokerSvcPing = createSvc(
      "/crts/invoker-svc-ping.yml",
      prefix + "invoker-ping",
      labels);
    var container = deployment.getSpec().getTemplate().getSpec()
      .getContainers().getFirst();
    addEnv(container, "ISPN_DNS_PING",
      invokerSvcPing.getMetadata().getName() + "." + namespace + ".svc.cluster.local");
    addEnv(container, "KUBERNETES_NAMESPACE", namespace);

    container.getEnv()
      .add(new EnvVar(
        "ISPN_POD_NAME",
        null,
        new EnvVarSource(null, new ObjectFieldSelector(null, "metadata.name"), null, null))
      );
    return List.of(deployment, podMonitor, invokerSvc, invokerSvcPing);
  }

  @Override
  protected List<HasMetadata> doCreateAdjustOperation(CrInstanceSpec instanceSpec) {
    String name = prefix + INVOKER.getSvc();
    var deployment = kubernetesClient.apps().deployments()
      .inNamespace(namespace)
      .withName(name).get();
    if (deployment==null) return List.of();
    deployment.getSpec().setReplicas(instanceSpec.minInstance());
    return List.of(deployment);
  }

  @Override
  public List<HasMetadata> createDeleteOperation() {
    List<HasMetadata> toDeleteResource = Lists.mutable.empty();
    String tsidString = parentController.getTsidString();
    var depList = kubernetesClient.apps().deployments()
      .withLabel(CR_LABEL_KEY, tsidString)
      .withLabel(CR_COMPONENT_LABEL_KEY, INVOKER.getSvc())
      .list()
      .getItems();
    toDeleteResource.addAll(depList);
    var svcList = kubernetesClient.services()
      .withLabel(CR_LABEL_KEY, tsidString)
      .withLabel(CR_COMPONENT_LABEL_KEY, INVOKER.getSvc())
      .list()
      .getItems();
    toDeleteResource.addAll(svcList);
    var podMonitor = kubernetesClient.genericKubernetesResources("monitoring.coreos.com/v1", "PodMonitor")
      .withLabel(CR_LABEL_KEY, tsidString)
      .list()
      .getItems();
    toDeleteResource.addAll(podMonitor);
    return toDeleteResource;
  }
}
