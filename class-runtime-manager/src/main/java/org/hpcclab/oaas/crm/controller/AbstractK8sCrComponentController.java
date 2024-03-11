package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public abstract class AbstractK8sCrComponentController implements CrComponentController<HasMetadata> {
  protected final CrtMappingConfig.SvcConfig svcConfig;
  protected K8SCrController parentController;
  protected KubernetesClient kubernetesClient;
  protected String prefix;
  protected String namespace;
  String svcName;
  long stabilizationTime;

  protected AbstractK8sCrComponentController(CrtMappingConfig.SvcConfig svcConfig) {
    this.svcConfig = svcConfig;
  }

  @Override
  public void init(CrController parentController) {
    if (parentController instanceof K8SCrController k8SCrController) {
      this.parentController = k8SCrController;
      this.kubernetesClient = k8SCrController.kubernetesClient;
      this.prefix = k8SCrController.prefix;
      this.namespace = k8SCrController.namespace;
    } else
      throw new IllegalArgumentException("Parent cr controller is not a K8SCrController");
  }

  @Override
  public List<HasMetadata> createAdjustOperation(CrInstanceSpec instanceSpec) {
    if (instanceSpec == null) return List.of();
    if (stabilizationTime > System.currentTimeMillis()) {
      return List.of();
    }
    stabilizationTime = System.currentTimeMillis() + svcConfig.stabilizationWindow();
    return doCreateAdjustOperation(instanceSpec);
  }

  @Override
  public void updateStabilizationTime() {
    stabilizationTime = System.currentTimeMillis() + svcConfig.stabilizationWindow();
  }

  protected abstract List<HasMetadata> doCreateAdjustOperation(CrInstanceSpec instanceSpec);

  protected Deployment createDeployment(String filePath,
                                        String name,
                                        Map<String, String> labels,
                                        CrInstanceSpec spec) {
    var is = getClass().getResourceAsStream(filePath);
    var image = svcConfig.image();
    var deployment = parentController.kubernetesClient.getKubernetesSerialization()
      .unmarshal(is, Deployment.class);
    deployment.getSpec()
      .setReplicas(spec.minInstance());
    Container container = deployment.getSpec()
      .getTemplate()
      .getSpec()
      .getContainers()
      .getFirst();
    container.setImage(image);
    if (svcConfig.imagePullPolicy()!=null && !svcConfig.imagePullPolicy().isEmpty())
      container.setImagePullPolicy(svcConfig.imagePullPolicy());
    container.setResources(K8sResourceUtil.makeResourceRequirements(spec));
    for (Map.Entry<String, String> entry : svcConfig.env().entrySet()) {
      addEnv(container, entry.getKey(), entry.getValue());
    }
    rename(deployment, name);
    attachLabels(deployment, labels);
    return deployment;
  }

  protected void rename(HasMetadata o,
                        String name) {
    var meta = o
      .getMetadata();
    meta.setName(name);
    meta.setNamespace(parentController.namespace);
  }

  protected void attachLabels(Service service,
                                     Map<String, String> labels) {
    var meta = service
      .getMetadata();
    meta.getLabels().putAll(labels);
    var spec = service.getSpec();
    spec
      .setSelector(labels);
  }


  protected void addEnv(Container container, String key, String val) {
    container.getEnv().add(new EnvVar(key, val, null));
  }
  protected void attachLabels(Deployment deployment,
                              Map<String, String> labels) {
    var meta = deployment
      .getMetadata();
    meta.getLabels().putAll(labels);
    var spec = deployment.getSpec();
    var specTemp = spec.getTemplate();
    spec
      .getSelector()
      .setMatchLabels(labels);
    specTemp.getMetadata()
      .getLabels()
      .putAll(labels);
    var aff = specTemp.getSpec().getAffinity()
      .getPodAntiAffinity()
      .getPreferredDuringSchedulingIgnoredDuringExecution();
    if (!aff.isEmpty()) {
      for (WeightedPodAffinityTerm weightedPodAffinityTerm : aff) {
        weightedPodAffinityTerm.getPodAffinityTerm()
          .getLabelSelector()
          .setMatchLabels(labels);
      }
    }
  }
  protected Service createSvc(String filePath,
                              String name,
                              Map<String, String> labels) {
    var is = getClass().getResourceAsStream(filePath);
    var service = parentController.kubernetesClient
      .getKubernetesSerialization()
      .unmarshal(is, Service.class);
    rename(service, name);
    attachLabels(service, labels);
    return service;
  }

  protected void attachSecret(Deployment deployment, String secretName) {
    var cons = deployment.getSpec()
      .getTemplate()
      .getSpec()
      .getContainers();
    for (Container container : cons) {
      container.getEnvFrom()
        .add(new EnvFromSource(null, null, new SecretEnvSource(secretName, false)));
    }
  }

  protected void attachConf(Deployment deployment, String confName) {
    var cons = deployment.getSpec()
      .getTemplate()
      .getSpec()
      .getContainers();
    for (Container container : cons) {
      container.getEnvFrom()
        .add(new EnvFromSource(new ConfigMapEnvSource(confName, false), null, null));
    }
  }
}

