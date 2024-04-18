package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class AdjustmentCrOperation extends ApplyK8SCrOperation{
  public AdjustmentCrOperation(KubernetesClient client,
                               List<? extends HasMetadata> k8sResources,
                               Runnable updater) {
    super(client, k8sResources, updater);
  }

  @Override
  public void apply() throws CrDeployException {
    originalResources = client.resourceList(k8sResources).get();
    client.resourceList(k8sResources)
      .update();
    if (updater!=null)
      updater.run();
  }

  @Override
  public void rollback() throws CrDeployException {

  }
}
