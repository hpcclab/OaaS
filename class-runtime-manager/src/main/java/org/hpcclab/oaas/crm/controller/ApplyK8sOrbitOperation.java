package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.env.OprcEnvironment.EnvResource;
import org.hpcclab.oaas.crm.exception.CrDeployException;

import java.util.List;
import java.util.Objects;

public class ApplyK8sOrbitOperation implements OrbitOperation {
  KubernetesClient client;
  List<? extends HasMetadata> k8sResources;
  List<? extends HasMetadata> originalResources;
  Runnable updater;

  public ApplyK8sOrbitOperation(KubernetesClient client,
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
      .serverSideApply();
    if (updater!=null)
      updater.run();
  }

  @Override
  public EnvResource estimate() {
    return k8sResources.stream()
      .filter(Deployment.class::isInstance)
      .map(d -> ((Deployment) d).getSpec()
        .getTemplate()
        .getSpec()
        .getContainers()
        .stream()
        .map(Container::getResources)
        .filter(Objects::nonNull)
        .map(ResourceRequirements::getRequests)
        .filter(Objects::nonNull)
        .map(EnvResource::new)
        .reduce(EnvResource.ZERO, EnvResource::sum)
        .mul(((Deployment) d).getSpec().getReplicas())
      )
      .reduce(EnvResource.ZERO, EnvResource::sum);
  }

  @Override
  public void rollback() throws CrDeployException {
    client.resourceList(originalResources)
      .serverSideApply();
    var resToDelete = Lists.mutable.ofAll(k8sResources);
    resToDelete.removeIf(checkResource ->
      originalResources.stream().anyMatch(original -> compare(original, checkResource)));
    client.resourceList(resToDelete)
      .delete();
  }

  boolean compare(HasMetadata r1, HasMetadata r2) {
    return Objects.equals(r1.getKind(), r2.getKind()) &&
      Objects.equals(r1.getMetadata().getName(), r2.getMetadata().getName()) &&
      Objects.equals(r1.getMetadata().getNamespace(), r2.getMetadata().getNamespace());
  }
}
