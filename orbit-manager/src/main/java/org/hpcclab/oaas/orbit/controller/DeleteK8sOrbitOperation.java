package org.hpcclab.oaas.orbit.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.exception.OrbitDeployException;

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
  public void apply() throws OrbitDeployException {
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
  public void rollback() throws OrbitDeployException {
    client.resourceList(originalResources)
      .serverSideApply();
  }
}
