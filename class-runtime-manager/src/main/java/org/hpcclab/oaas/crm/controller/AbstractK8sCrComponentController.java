package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.autoscaling.v2.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.filter.CrFilter;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public abstract class AbstractK8sCrComponentController implements CrComponentController<HasMetadata> {
  protected final CrtMappingConfig.CrComponentConfig svcConfig;
  protected final OprcEnvironment.Config envConfig;
  protected K8SCrController parentController;
  protected KubernetesClient kubernetesClient;
  protected String prefix;
  protected String namespace;
  long stableTime;
  List<CrFilter<List<HasMetadata>>> filters = new ArrayList<>();

  protected AbstractK8sCrComponentController(CrtMappingConfig.CrComponentConfig svcConfig,
                                             OprcEnvironment.Config envConfig) {
    this.envConfig = envConfig;
    if (svcConfig==null) {
      this.svcConfig = CrtMappingConfig.CrComponentConfig.builder()
        .build();
    } else {
      this.svcConfig = svcConfig;
    }
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
  public List<HasMetadata> createDeployOperation(CrDeploymentPlan plan) {
    List<HasMetadata> hasMetadata = doCreateDeployOperation(plan);
    for (CrFilter<List<HasMetadata>> filter : filters) {
      hasMetadata = filter.applyOnCreate(hasMetadata);
    }
    return hasMetadata;
  }

  protected abstract List<HasMetadata> doCreateDeployOperation(CrDeploymentPlan plan);

  @Override
  public List<HasMetadata> createAdjustOperation(CrAdjustmentPlan plan) {
    if (plan==null) return List.of();
    if (stableTime > System.currentTimeMillis()) {
      return List.of();
    }
    List<HasMetadata> hasMetadata = doCreateAdjustOperation(plan);
    for (CrFilter<List<HasMetadata>> filter : filters) {
      hasMetadata = filter.applyOnAdjust(hasMetadata);
    }
    if (!hasMetadata.isEmpty()) {
      stableTime = System.currentTimeMillis() + svcConfig.stabilizationWindow();
    }
    return hasMetadata;
  }

  @Override
  public void updateStableTime() {
    stableTime = System.currentTimeMillis() + svcConfig.stabilizationWindow();
  }


  protected abstract List<HasMetadata> doCreateAdjustOperation(CrAdjustmentPlan plan);

  @Override
  public List<HasMetadata> createDeleteOperation() {
    List<HasMetadata> list = doCreateDeleteOperation();
    for (CrFilter<List<HasMetadata>> filter : filters) {
      list = filter.applyOnDelete(list);
    }
    return list;
  }

  protected abstract List<HasMetadata> doCreateDeleteOperation();

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


  protected HorizontalPodAutoscaler createHpa(CrInstanceSpec spec,
                                              Map<String, String> labels,
                                              String name,
                                              String deployName) {
    HorizontalPodAutoscalerBehavior behavior = new HorizontalPodAutoscalerBehaviorBuilder()
      .withNewScaleDown()
      .addToPolicies(new HPAScalingPolicyBuilder()
        .withType("Pods")
        .withValue(1)
        .withPeriodSeconds(30)
        .build()
      )
      .endScaleDown()
      .withNewScaleUp()
      .addToPolicies(new HPAScalingPolicyBuilder()
        .withType("Percent")
        .withValue(10)
        .withPeriodSeconds(15)
        .build()
      )
      .addToPolicies(new HPAScalingPolicyBuilder()
        .withType("Pods")
        .withValue(2)
        .withPeriodSeconds(15)
        .build()
      )
      .withSelectPolicy("Max")
      .withStabilizationWindowSeconds(
        svcConfig.stabilizationWindow() > 0 ?
          svcConfig.stabilizationWindow() / 1000:15
      )
      .endScaleUp()
      .build();
    MetricSpec metricSpec = new MetricSpecBuilder()
      .withType("Resource")
      .withNewResource()
      .withName("cpu")
      .withNewTarget()
      .withType("Utilization")
      .withAverageUtilization(100)
      .endTarget()
      .endResource()
      .build();
    return new HorizontalPodAutoscalerBuilder()
      .withNewMetadata()
      .withName(name)
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withNewSpec()
      .withNewScaleTargetRef()
      .withKind("Deployment")
      .withApiVersion("apps/v1")
      .withName(deployName)
      .endScaleTargetRef()
      .withMinReplicas(spec.minInstance())
      .withMaxReplicas(spec.maxInstance())
      .withBehavior(behavior)
      .withMetrics(metricSpec)
      .endSpec()
      .build();
  }

  protected HorizontalPodAutoscaler editHpa(CrInstanceSpec spec,
                                            String name) {
    HorizontalPodAutoscaler hpa = kubernetesClient.autoscaling().v2().horizontalPodAutoscalers()
      .inNamespace(namespace)
      .withName(name).get();
    if (hpa==null) return null;
    hpa.getSpec()
      .setMinReplicas(spec.minInstance());
    hpa.getSpec()
      .setMaxReplicas(spec.maxInstance());
    return hpa;
  }

  @Override
  public long getStableTime() {
    return stableTime;
  }

  @Override
  public void addFilter(CrFilter<List<HasMetadata>> newFilter) {
    this.filters.add(newFilter);
  }
}

