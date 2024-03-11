package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.CrtMappingConfig;
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
  public List<HasMetadata> createDeployOperation(CrInstanceSpec instanceSpec) {
    var labels = Map.of(
      CR_LABEL_KEY, parentController.getTsidString(),
      CR_COMPONENT_LABEL_KEY, STORAGE_ADAPTER.getSvc()
    );
    var deployment = createDeployment(
      "/crts/storage-adapter-dep.yml",
      prefix + STORAGE_ADAPTER.getSvc(),
      labels,
      instanceSpec
    );
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var svc = createSvc(
      "/crts/storage-adapter-svc.yml",
      prefix + STORAGE_ADAPTER.getSvc(),
      labels);
    return List.of(deployment, svc);
  }

  @Override
  protected List<HasMetadata> doCreateAdjustOperation(CrInstanceSpec instanceSpec) {
    String name = prefix + STORAGE_ADAPTER.getSvc();
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
      .withLabel(CR_COMPONENT_LABEL_KEY, STORAGE_ADAPTER.getSvc())
      .list()
      .getItems();
    toDeleteResource.addAll(depList);
    var svcList = kubernetesClient.services()
      .withLabel(CR_LABEL_KEY, tsidString)
      .withLabel(CR_COMPONENT_LABEL_KEY, INVOKER.getSvc())
      .list()
      .getItems();
    toDeleteResource.addAll(svcList);
    return toDeleteResource;
  }
}
