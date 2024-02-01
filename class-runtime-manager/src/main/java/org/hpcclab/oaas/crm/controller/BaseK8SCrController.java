package org.hpcclab.oaas.crm.controller;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.crm.OrbitTemplate;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.exception.CrUpdateException;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseK8SCrController implements CrController {
  private static final Logger logger = LoggerFactory.getLogger(BaseK8SCrController.class);
  long id;
  String prefix;
  String namespace;
  OrbitTemplate template;
  KubernetesClient kubernetesClient;
  OprcEnvironment.Config envConfig;

  Set<String> attachedCls = Sets.mutable.empty();
  Set<String> attachedFn = Sets.mutable.empty();
  Map<String, Long> versions = Maps.mutable.empty();
  List<HasMetadata> k8sResources = Lists.mutable.empty();

  static final String ORBIT_LABEL_KEY = "orbit-id";
  static final String ORBIT_COMPONENT_LABEL_KEY = "orbit-part";
  static final String ORBIT_FN_KEY = "orbit-fn";

  protected BaseK8SCrController(OrbitTemplate template,
                                KubernetesClient client,
                                OprcEnvironment.Config envConfig,
                                Tsid tsid) {
    this.template = template;
    this.kubernetesClient = client;
    this.envConfig = envConfig;
    namespace = kubernetesClient.getNamespace();
    id = tsid.toLong();
    prefix = "orbit-" + tsid.toLowerCase() + "-";
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public Set<String> getAttachedCls() {
    return attachedCls;
  }

  @Override
  public Set<String> getAttachedFn() {
    return attachedFn;
  }

  @Override
  public OrbitOperation createUpdateOperation(CrDeploymentPlan plan, DeploymentUnit unit) {
    List<HasMetadata> resources = Lists.mutable.empty();
    for (var f : unit.getFnListList()) {
      if (!attachedFn.contains(f.getKey()))
        resources.addAll(deployFunction(plan, f));
    }
    return new ApplyK8sOrbitOperation(kubernetesClient, resources, () -> {
      for (var f : unit.getFnListList()) {
        attachedFn.add(f.getKey());
      }
      k8sResources.addAll(resources);
    });
  }

  @Override
  public CrDeploymentPlan createPlan(DeploymentUnit unit) {
    return template.getQosOptimizer().resolve(unit);
  }

  @Override
  public OrbitOperation createDeployOperation(CrDeploymentPlan plan, DeploymentUnit unit)
    throws CrDeployException {
    List<HasMetadata> resourceList = Lists.mutable.empty();
    resourceList.addAll(deployShared(plan));
    resourceList.addAll(deployDataModule(plan));
    resourceList.addAll(deployExecutionModule(plan));
    resourceList.addAll(deployObjectModule(plan, unit));
    for (ProtoOFunction fn : unit.getFnListList()) {
      resourceList.addAll(deployFunction(plan, fn));
    }
    return new ApplyK8sOrbitOperation(kubernetesClient, resourceList, () -> {
      attachedCls.add(unit.getCls().getKey());
      for (ProtoOFunction fn : unit.getFnListList()) {
        attachedFn.add(fn.getKey());
      }
      k8sResources.addAll(resourceList);
    });
  }


  public List<? extends HasMetadata> deployShared(CrDeploymentPlan plan) throws CrDeployException {
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id)
    );
    var datastoreMap = DatastoreConfRegistry.getDefault().dump();
    var sec = new SecretBuilder()
      .withNewMetadata()
      .withName(prefix + "secret")
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withStringData(datastoreMap)
      .build();

    var confMapData = Map.of(
      "OPRC_INVOKER_KAFKA", envConfig.kafkaBootstrap(),
      "OPRC_INVOKER_STORAGEADAPTERURL", "http://%s-storage-adapter.%s.svc.cluster.local"
        .formatted(prefix, namespace),
      "OPRC_ORBIT", Tsid.from(id).toLowerCase(),
      "OPRC_INVOKER_CLASSMANAGERHOST", envConfig.classManagerHost(),
      "OPRC_INVOKER_CLASSMANAGERPORT", envConfig.classManagerPort()
    );
    var confMap = new ConfigMapBuilder()
      .withNewMetadata()
      .withName(prefix + "cm")
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withData(confMapData)
      .build();

    return List.of(confMap, sec);
  }

  public List<? extends HasMetadata> deployObjectModule(CrDeploymentPlan plan, DeploymentUnit unit) {
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id),
      ORBIT_COMPONENT_LABEL_KEY, "invoker"
    );
    var deployment = createDeployment(
      "/orbits/invoker-dep.yml",
      prefix + "invoker",
      labels);
    attachSecret(deployment, prefix + "secret");
    attachConf(deployment, prefix + "cm");
    var invokerSvc = createSvc(
      "/orbits/invoker-svc.yml",
      prefix + "invoker",
      labels);
    var invokerSvcPing = createSvc(
      "/orbits/invoker-svc-ping.yml",
      prefix + "invoker-ping",
      labels);
    var container = deployment.getSpec().getTemplate().getSpec()
      .getContainers().getFirst();
    addEnv(container, "ISPN_DNS_PING",
      invokerSvcPing.getMetadata().getName() + "." + namespace + ".svc.cluster.local");
    addEnv(container, "KUBERNETES_NAMESPACE", namespace);
    container.getEnv()
      .add(new EnvVar(
        "ISPN_POD_NAME",
        null,
        new EnvVarSource(null, new ObjectFieldSelector(null, "metadata.name"), null, null))
      );
    return List.of(deployment, invokerSvc, invokerSvcPing);
  }

  public List<HasMetadata> deployExecutionModule(CrDeploymentPlan plan) {
    // no needed
    return List.of();
  }

  public List<HasMetadata> deployDataModule(CrDeploymentPlan plan) throws CrDeployException {
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id),
      ORBIT_COMPONENT_LABEL_KEY, "storage-adapter"
    );
    var deployment = createDeployment(
      "/orbits/storage-adapter-dep.yml",
      prefix + "storage-adapter",
      labels);
    attachSecret(deployment, prefix + "secret");
    attachConf(deployment, prefix + "cm");
    var svc = createSvc(
      "/orbits/storage-adapter-svc.yml",
      prefix + "storage-adapter",
      labels);
    return List.of(deployment, svc);
  }

  public abstract List<HasMetadata> deployFunction(CrDeploymentPlan plan,
                                      ProtoOFunction function) throws CrDeployException;

  public abstract List<HasMetadata> removeFunction(String fnKey) throws CrUpdateException;

  @Override
  public OrbitOperation createDetachOperation(ProtoOClass cls) throws CrUpdateException {
    // TODO send signal
    if (attachedCls.size() == 1 && attachedCls.contains(cls.getKey())) {
      return createDestroyOperation();
    }
    List<HasMetadata> resourceList = Lists.mutable.empty();
    for (ProtoFunctionBinding fb : cls.getFunctionsList()) {
      resourceList.addAll(removeFunction(fb.getFunction()));
    }
    return new DeleteK8sOrbitOperation(kubernetesClient, resourceList, () -> {
      attachedCls.remove(cls.getKey());
      for (ProtoFunctionBinding fb : cls.getFunctionsList()) {
        attachedFn.remove(fb.getFunction());
      }
      k8sResources.removeAll(resourceList);
    });
  }

  @Override
  public OrbitOperation createDestroyOperation() throws CrUpdateException {
    if (k8sResources.isEmpty()) {
      var depList = kubernetesClient.apps().deployments()
        .withLabel(ORBIT_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(depList);
      var svcList = kubernetesClient.services()
        .withLabel(ORBIT_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(svcList);
      var sec = kubernetesClient.secrets()
        .withLabel(ORBIT_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(sec);
      var configMaps = kubernetesClient.configMaps()
        .withLabel(ORBIT_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(configMaps);
    }
    return new DeleteK8sOrbitOperation(kubernetesClient, k8sResources,
      () -> k8sResources.clear());
  }

  protected Deployment createDeployment(String filePath,
                                        String name,
                                        Map<String, String> labels) {
    var is = getClass().getResourceAsStream(filePath);
    var deployment = kubernetesClient.getKubernetesSerialization()
      .unmarshal(is, Deployment.class);
    rename(deployment, name);
    attachLabels(deployment, labels);
    return deployment;
  }

  @Override
  public ProtoCr dump() {
    return ProtoCr.newBuilder()
      .setId(id)
      .setType(template.type())
      .setNamespace(namespace)
      .addAllAttachedCls(attachedCls)
      .addAllAttachedFn(attachedFn)
      .build();
  }

  protected Service createSvc(String filePath,
                              String name,
                              Map<String, String> labels) {
    var is = getClass().getResourceAsStream(filePath);
    var service = kubernetesClient.getKubernetesSerialization()
      .unmarshal(is, Service.class);
    rename(service, name);
    attachLabels(service, labels);
    return service;
  }

  protected void rename(HasMetadata o,
                        String name) {
    var meta = o
      .getMetadata();
    meta.setName(name);
    meta.setNamespace(namespace);
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

  protected void attachLabels(Service service,
                              Map<String, String> labels) {
    var meta = service
      .getMetadata();
    meta.getLabels().putAll(labels);
    var spec = service.getSpec();
    spec
      .setSelector(labels);
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

  protected void addEnv(Container container, String key, String val) {
    container.getEnv().add(new EnvVar(key, val, null));
  }
}
