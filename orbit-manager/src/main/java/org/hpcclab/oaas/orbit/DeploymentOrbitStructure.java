package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.exception.OrbitDeployException;
import org.hpcclab.oaas.orbit.exception.OrbitUpdateException;
import org.hpcclab.oaas.orbit.optimize.OrbitDeploymentPlan;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeploymentOrbitStructure implements OrbitStructure {
  private static final Logger logger = LoggerFactory.getLogger(DeploymentOrbitStructure.class);
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
  public void update(OrbitDeploymentPlan plan, DeploymentUnit unit) {
    for (var f: unit.getFnListList()) {
      if (!attachedFn.contains(f.getKey()))
        deployFunction(plan, f);
    }
  }

  @Override
  public OrbitDeploymentPlan createPlan(DeploymentUnit unit) {
    return template.getQosOptimizer().resolve(unit);
  }

  @Override
  public void deployShared(OrbitDeploymentPlan plan) throws OrbitDeployException {
    var labels = Map.of(
      ORBIT_LABEL_KEY, String.valueOf(id)
    );
    kubernetesClient.top()
      .nodes().metric().getUsage();
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
      "OPRC_ORBIT", Tsid.from(id).toLowerCase(),
      "OPRC_INVOKER_CLASSMANAGERHOST", envConfig.classManagerHost(),
      "OPRC_INVOKER_CLASSMANAGERPORT", envConfig.classManagerPort()
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
  public void deployObjectModule(OrbitDeploymentPlan plan, DeploymentUnit unit)
    throws OrbitDeployException {
    try {
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
        invokerSvcPing.getMetadata().getName() + "." + namespace + ".svc.cluster.local");
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
      attachedCls.add(unit.getCls().getKey());
    } catch (Exception e) {
      throw new OrbitDeployException(e);
    }
  }

  @Override
  public void deployExecutionModule(OrbitDeploymentPlan plan) throws OrbitDeployException {
    // no needed
  }

  @Override
  public void deployDataModule(OrbitDeploymentPlan plan) throws OrbitDeployException {
    try {
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
    } catch (Exception e) {
      throw new OrbitDeployException(e);
    }
  }

  @Override
  public void deployFunction(OrbitDeploymentPlan plan,
                             ProtoOFunction function) throws OrbitDeployException {
    try {
      var instance = plan.fnInstances()
        .getOrDefault(function.getKey(), 0);
      var labels = Map.of(
        ORBIT_LABEL_KEY, String.valueOf(id),
        ORBIT_COMPONENT_LABEL_KEY, "function",
        ORBIT_FN_KEY, function.getKey()
      );
      var deployConf = function.getProvision()
        .getDeployment();
      if (deployConf.getImage()==null || deployConf.getImage().isEmpty())
        return;
      var container = new ContainerBuilder()
        .withName("fn")
        .withImage(deployConf.getImage())
        .addAllToEnv(deployConf.getEnvMap()
          .entrySet().stream().map(e -> new EnvVar(e.getKey(), e.getValue(), null))
          .toList()
        )
        .withPorts(new ContainerPortBuilder()
          .withName("http")
          .withProtocol("TCP")
          .withContainerPort(deployConf.getPort() <= 0 ? 8080:deployConf.getPort())
          .build()
        )
        .build();
      var fnName = prefix + function.getKey().toLowerCase().replaceAll("[\\._]", "-");
      var deploymentBuilder = new DeploymentBuilder()
        .withNewMetadata()
        .withName(fnName)
        .withLabels(labels)
        .endMetadata();
      deploymentBuilder
        .withNewSpec()
        .withReplicas(instance)
        .withNewSelector()
        .addToMatchLabels(labels)
        .endSelector()
        .withNewTemplate()
        .withNewMetadata()
        .addToLabels(labels)
        .endMetadata()
        .withNewSpec()
        .addToContainers(container)
        .endSpec()
        .endTemplate()
        .endSpec();
      var deployment = deploymentBuilder.build();
      var svc = new ServiceBuilder()
        .withNewMetadata()
        .withName(fnName)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .addToSelector(labels)
        .addToPorts(
          new ServicePortBuilder()
            .withName("http")
            .withProtocol("TCP")
            .withPort(80)
            .withTargetPort(new IntOrString(deployConf.getPort() <= 0 ? 8080:deployConf.getPort()))
            .build()
        )
        .endSpec()
        .build();

      k8sResources.add(deployment);
      k8sResources.add(svc);
      kubernetesClient.resourceList(deployment, svc)
        .create();
      attachedFn.add(function.getKey());

    } catch (Exception e) {
      throw new OrbitDeployException(e);
    }
  }

  public void removeFunction(String fnKey) throws OrbitUpdateException {
    var list = kubernetesClient.apps()
      .deployments()
      .withLabels(Map.of(
        ORBIT_LABEL_KEY, String.valueOf(id),
        ORBIT_FN_KEY, fnKey
      ))
      .list().getItems();
    kubernetesClient.resourceList(list).delete();
    attachedFn.remove(fnKey);
  }

  @Override
  public void detach(ProtoOClass cls) throws OrbitUpdateException {
    // TODO send signal
    attachedCls.remove(cls.getKey());
    if (attachedCls.isEmpty()) {
      destroy();
    }
    for (ProtoFunctionBinding fb : cls.getFunctionsList()) {
      removeFunction(fb.getFunction());
    }
  }

  @Override
  public void destroy() throws OrbitUpdateException {
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
      .addAllAttachedFn(attachedFn)
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
