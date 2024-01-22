package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOrbit;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeploymentOrbitStructure implements OrbitStructure {
  private static final Logger logger = LoggerFactory.getLogger( DeploymentOrbitStructure.class );
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

  public DeploymentOrbitStructure(OrbitTemplate template,
                                  KubernetesClient client,
                                  OprcEnvironment.Config envConfig,
                                  Tsid tsid) {
    this.template = template;
    this.kubernetesClient = client;
    this.envConfig = envConfig;
    namespace = kubernetesClient.getNamespace();
    id = tsid.toLong();
    prefix = "orbit-" + tsid.toLowerCase();
  }

  public DeploymentOrbitStructure(OrbitTemplate template,
                                  KubernetesClient client,
                                  OprcEnvironment.Config envConfig,
                                  ProtoOrbit orbit) {
    this.template = template;
    this.kubernetesClient = client;
    this.envConfig = envConfig;
    namespace = kubernetesClient.getNamespace();
    id = orbit.getId();
    prefix = "orbit-" + Tsid.from(orbit.getId()).toLowerCase();
    attachedCls.addAll(orbit.getAttachedClsList());
    attachedFn.addAll(orbit.getAttachedFnList());
  }

  public DeploymentOrbitStructure attach(DeploymentUnit unit) {
    attachedCls.add(unit.getCls().getKey());
    return this;
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
  public void update(DeploymentUnit unit) {

  }

  @Override
  public void deployShared() throws Throwable {
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id)
    );
    var datastoreMap = DatastoreConfRegistry.getDefault().dump();
    var sec = new SecretBuilder()
      .withNewMetadata()
      .withName(prefix + "-secret")
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withStringData(datastoreMap)
      .build();
    kubernetesClient.secrets().resource(sec).create();
    k8sResources.add(sec);
    var confMapData = Map.of(
      "OPRC_INVOKER_KAFKA", envConfig.kafkaBootstrap(),
      "OPRC_INVOKER_STORAGEADAPTERURL", "http://%s-storage-adapter.%s.svc.cluster.local"
        .formatted(prefix, namespace),
      "OPRC_ORBIT", Tsid.from(id).toLowerCase()
    );
    var confMap = new ConfigMapBuilder()
      .withNewMetadata()
      .withName(prefix + "-cm")
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withData(confMapData)
      .build();
    kubernetesClient.configMaps().resource(confMap).create();
    k8sResources.add(confMap);

  }

  @Override
  public void deployObjectModule() throws Throwable {
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id),
      ORBIT_COMPONENT_LABEL_KEY, "invoker"
    );
    var deployment = createDeployment(
      "/orbits/invoker-dep.yml",
      prefix + "-invoker",
      labels);
    attachSecret(deployment, prefix + "-secret");
    attachConf(deployment, prefix + "-cm");
    var invokerSvc = createSvc(
      "/orbits/invoker-svc.yml",
      prefix + "-invoker",
      labels);
    var invokerSvcPing = createSvc(
      "/orbits/invoker-svc-ping.yml",
      prefix + "-invoker-ping",
      labels);
    var container = deployment.getSpec().getTemplate().getSpec()
      .getContainers().getFirst();
    addEnv(container, "ISPN_DNS_PING",
      invokerSvcPing.getMetadata().getName() + "." + namespace + ".cluster.local");
    addEnv(container, "KUBERNETES_NAMESPACE", namespace);
    container.getEnv()
      .add(new EnvVar(
        "ISPN_POD_NAME",
        null,
        new EnvVarSource(null, new ObjectFieldSelector(null, "metadata.name"), null, null))
      );

    kubernetesClient.resourceList(deployment, invokerSvc, invokerSvcPing)
      .create();
    k8sResources.add(deployment);
    k8sResources.add(invokerSvc);
    k8sResources.add(invokerSvcPing);
  }

  @Override
  public void deployExecutionModule() throws Throwable {
    // no needed
  }

  @Override
  public void deployDataModule() throws Throwable {
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id),
      ORBIT_COMPONENT_LABEL_KEY, "storage-adapter"
    );
    var deployment = createDeployment(
      "/orbits/storage-adapter-dep.yml",
      prefix + "-storage-adapter",
      labels);
    attachSecret(deployment, prefix + "-secret");
    attachConf(deployment, prefix + "-cm");
    var svc = createSvc(
      "/orbits/storage-adapter-svc.yml",
      prefix + "-storage-adapter",
      labels);
    kubernetesClient.resourceList(deployment, svc)
      .create();
    k8sResources.add(deployment);
    k8sResources.add(svc);
  }

  @Override
  public void deployFunction(ProtoOFunction function) throws Throwable {

  }

  public void removeFunction(String fnKey) throws Throwable {
    // TODO
  }

  @Override
  public void detach(ProtoOClass cls) throws Throwable {
    // TODO send signal
    attachedCls.removeIf(key -> key.equals(cls.getKey()));
    if (attachedCls.isEmpty()) {
      destroy();
    }
  }

  @Override
  public void destroy() throws Throwable {
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
    kubernetesClient.resourceList(k8sResources)
      .delete();
  }

  private Deployment createDeployment(String filePath,
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
  public ProtoOrbit dump() {
    return ProtoOrbit.newBuilder()
      .setId(id)
      .setType(template.type())
      .setNamespace(namespace)
      .addAllAttachedCls(attachedCls)
      .addAllAttachedCls(attachedFn)
      .build();
  }

  private Service createSvc(String filePath,
                            String name,
                            Map<String, String> labels) {
    var is = getClass().getResourceAsStream(filePath);
    var service = kubernetesClient.getKubernetesSerialization()
      .unmarshal(is, Service.class);
    rename(service, name);
    attachLabels(service, labels);
    return service;
  }

  private void rename(HasMetadata o,
                      String name) {
    var meta = o
      .getMetadata();
    meta.setName(name);
    meta.setNamespace(namespace);
  }

  private void attachLabels(Deployment deployment,
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

  private void attachLabels(Service service,
                            Map<String, String> labels) {
    var meta = service
      .getMetadata();
    meta.getLabels().putAll(labels);
    var spec = service.getSpec();
    spec
      .setSelector(labels);
  }

  private void attachSecret(Deployment deployment, String secretName) {
    var cons = deployment.getSpec()
      .getTemplate()
      .getSpec()
      .getContainers();
    for (Container container : cons) {
      container.getEnvFrom()
        .add(new EnvFromSource(null, null, new SecretEnvSource(secretName, false)));
    }
  }

  private void attachConf(Deployment deployment, String confName) {
    var cons = deployment.getSpec()
      .getTemplate()
      .getSpec()
      .getContainers();
    for (Container container : cons) {
      container.getEnvFrom()
        .add(new EnvFromSource(new ConfigMapEnvSource(confName, false), null, null));
    }
  }

  private void addEnv(Container container, String key, String val) {
    container.getEnv().add(new EnvVar(key, val, null));
  }
}
