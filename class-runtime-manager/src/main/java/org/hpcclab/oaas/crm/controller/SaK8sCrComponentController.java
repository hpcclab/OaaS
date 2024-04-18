package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.CrDataSpec;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.crm.OprcComponent.INVOKER;
import static org.hpcclab.oaas.crm.OprcComponent.STORAGE_ADAPTER;
import static org.hpcclab.oaas.crm.controller.K8SCrController.*;

/**
 * @author Pawissanutt
 */
public class SaK8sCrComponentController extends AbstractK8sCrComponentController{
  public SaK8sCrComponentController(CrtMappingConfig.SvcConfig svcConfig) {
    super(svcConfig);
  }

  @Override
  public List<HasMetadata> createDeployOperation(CrInstanceSpec instanceSpec, CrDataSpec dataSpec) {
    if (instanceSpec.disable()) return List.of();
    var labels = Map.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_COMPONENT_LABEL_KEY, STORAGE_ADAPTER.getSvc()
    );
    String name = prefix + STORAGE_ADAPTER.getSvc();
    var deployment = createDeployment(
      "/crts/storage-adapter-dep.yml",
      name,
      labels,
      instanceSpec
    );
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var svc = createSvc(
      "/crts/storage-adapter-svc.yml",
      name,
      labels);

    var hpa = createHpa(instanceSpec, labels, name, name);
    return List.of(deployment, svc, hpa);
  }

  @Override
  protected List<HasMetadata> doCreateAdjustOperation(CrInstanceSpec instanceSpec) {
    String name = prefix + STORAGE_ADAPTER.getSvc();
    HorizontalPodAutoscaler hpa = editHpa(instanceSpec, name);
    return hpa == null? List.of(): List.of(hpa);
  }

  @Override
  public List<HasMetadata> createDeleteOperation() {
    List<HasMetadata> toDeleteResource = Lists.mutable.empty();
    String tsidString = parentController.getTsidString();
    Map<String, String> labels = Map.of(
      CR_LABEL_KEY, tsidString,
      CR_COMPONENT_LABEL_KEY, STORAGE_ADAPTER.getSvc()
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
    var hpa = kubernetesClient.autoscaling().v2().horizontalPodAutoscalers()
      .withLabels(labels)
      .list().getItems();
    toDeleteResource.addAll(hpa);
    return toDeleteResource;
  }
}
