package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.autoscaling.v2.*;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.CrDataSpec;
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
  public List<HasMetadata> createDeployOperation(CrInstanceSpec instanceSpec, CrDataSpec dataSpec) {
    if (instanceSpec.disable()) return List.of();
    var labels = Map.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_COMPONENT_LABEL_KEY, INVOKER.getSvc()
    );
    String name = prefix + INVOKER.getSvc();
    var deployment = createDeployment(
      "/crts/invoker-dep.yml",
      name,
      labels,
      instanceSpec
    );
    var podMonitor = K8sResourceUtil
      .createPodMonitor(name, namespace, labels);
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var invokerSvc = createSvc(
      "/crts/invoker-svc.yml",
      name,
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
    addEnv(container, "OPRC_ISPN_OBJSTORE_OWNER", String.valueOf(dataSpec.replication()));

    container.getEnv()
      .add(new EnvVar(
        "ISPN_POD_NAME",
        null,
        new EnvVarSource(null, new ObjectFieldSelector(null, "metadata.name"), null, null))
      );
    var hpa = createHpa(instanceSpec, labels, name, name);
    return List.of(deployment, podMonitor, invokerSvc, invokerSvcPing, hpa);
  }


  @Override
  protected List<HasMetadata> doCreateAdjustOperation(CrInstanceSpec instanceSpec) {
    String name = prefix + INVOKER.getSvc();
    HorizontalPodAutoscaler hpa = editHpa(instanceSpec, name);
    return hpa == null? List.of(): List.of(hpa);
  }

  @Override
  public List<HasMetadata> createDeleteOperation() {
    List<HasMetadata> toDeleteResource = Lists.mutable.empty();
    String tsidString = parentController.getTsidString();
    Map<String, String> labels = Map.of(
      CR_LABEL_KEY, tsidString,
      CR_COMPONENT_LABEL_KEY, INVOKER.getSvc()
    );
    var depList = kubernetesClient.apps().deployments()
      .withLabels(labels)
      .list()
      .getItems();
    toDeleteResource.addAll(depList);
    var svcList = kubernetesClient.services()
      .withLabels(labels)
      .list()
      .getItems();
    toDeleteResource.addAll(svcList);
    var podMonitor = kubernetesClient.genericKubernetesResources("monitoring.coreos.com/v1", "PodMonitor")
      .withLabel(CR_LABEL_KEY, tsidString)
      .list()
      .getItems();
    toDeleteResource.addAll(podMonitor);
    var hpa = kubernetesClient.autoscaling().v2().horizontalPodAutoscalers()
      .withLabels(labels)
      .list().getItems();
    toDeleteResource.addAll(hpa);
    return toDeleteResource;
  }
}