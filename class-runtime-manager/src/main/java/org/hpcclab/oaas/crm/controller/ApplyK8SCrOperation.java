package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.env.OprcEnvironment.EnvResource;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.proto.OClassStatusUpdate;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;

import java.util.List;
import java.util.Objects;

public class ApplyK8SCrOperation implements CrOperation {
  KubernetesClient client;
  List<? extends HasMetadata> k8sResources;
  List<? extends HasMetadata> originalResources;
  Runnable updater;
  List<OFunctionStatusUpdate> fnUpdates = Lists.mutable.empty();
  List<OClassStatusUpdate> clsUpdates = Lists.mutable.empty();

  public ApplyK8SCrOperation(KubernetesClient client,
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
    if (originalResources != null && originalResources.isEmpty())
      client.resourceList(originalResources)
        .serverSideApply();
    var resToDelete = Lists.mutable.ofAll(k8sResources);
    if (originalResources != null && originalResources.isEmpty())
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

  @Override
  public StateUpdateOperation stateUpdates() {
    return new StateUpdateOperation(fnUpdates, clsUpdates);
  }

  public List<OFunctionStatusUpdate> getFnUpdates() {
    return fnUpdates;
  }

  public List<OClassStatusUpdate> getClsUpdates() {
    return clsUpdates;
  }
}
