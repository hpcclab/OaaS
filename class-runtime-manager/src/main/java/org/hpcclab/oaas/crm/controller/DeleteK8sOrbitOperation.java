package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;

import java.util.List;

public class DeleteK8sOrbitOperation implements OrbitOperation {
  KubernetesClient client;
  List<? extends HasMetadata> k8sResources;
  List<? extends HasMetadata> originalResources;
  Runnable updater;

  public DeleteK8sOrbitOperation(KubernetesClient client,
                                 List<? extends HasMetadata> k8sResources,
                                 Runnable updater) {
    this.client = client;
    this.k8sResources = k8sResources;
    this.updater = updater;
  }

  @Override
  public void apply() throws CrDeployException {
    originalResources = client.resourceList(k8sResources).get();
    client.resourceList(k8sResources)
      .delete();
    if (updater != null)
      updater.run();
  }


  @Override
  public OprcEnvironment.EnvResource estimate() {
    return OprcEnvironment.EnvResource.ZERO;
  }

  @Override
  public void rollback() throws CrDeployException {
    client.resourceList(originalResources)
      .serverSideApply();
  }
}
