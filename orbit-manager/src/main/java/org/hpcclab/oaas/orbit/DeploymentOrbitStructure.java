package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOrbit;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;

import java.util.List;
import java.util.Map;

public class DeploymentOrbitStructure implements OrbitStructure {
  long id;
  String prefix;
  String namespace;
  OrbitTemplate template;
  KubernetesClient kubernetesClient;
  List<String> attachedCls = Lists.mutable.empty();
  List<String> attachedFn = Lists.mutable.empty();
  Map<String, Long> versions = Maps.mutable.empty();
  List<HasMetadata> k8sResources = Lists.mutable.empty();

  static final String ORBIT_LABEL_KEY = "orbit-id";
  static final String ORBIT_COMPONENT_LABEL_KEY = "orbit-part";

  public DeploymentOrbitStructure(OrbitTemplate template,
                                  KubernetesClient client,
                                  Tsid tsid) {
    this.template = template;
    this.kubernetesClient = client;
    id = tsid.toLong();
    prefix = "orbit-" + tsid;
  }
  public DeploymentOrbitStructure(OrbitTemplate template,
                                  KubernetesClient client,
                                  ProtoOrbit orbit) {
    this.template = template;
    this.kubernetesClient = client;
    id = orbit.getId();
    prefix = "orbit-" + Tsid.from(orbit.getId());
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
  public List<String> getAttachedCls() {
    return attachedCls;
  }

  @Override
  public List<String> getAttachedFn() {
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
    var map = DatastoreConfRegistry.getDefault().dump();
    var sec = new SecretBuilder()
      .withNewMetadata()
      .withName(prefix + "-secret")
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withData(map)
      .build();
    kubernetesClient.secrets().resource(sec).create();
    k8sResources.add(sec);
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
    container.getEnv()
      .add(new EnvVar(
        "ISPN_DNS_PING",
        invokerSvcPing.getMetadata().getName() + "." + namespace + ".cluster.local",
        null)
      );
    container.getEnv()
      .add(new EnvVar(
        "KUBERNETES_NAMESPACE",
        namespace,
        null)
      );
    container.getEnv()
      .add(new EnvVar(
        "ISPN_POD_NAME",
        null,
        new EnvVarSource(null, new ObjectFieldSelector(null, "metadata.name"), null, null))
      );
    container.getEnv()
      .add(new EnvVar(
        "OAAS_INVOKER_STORAGEADAPTERURL",
        "http://" + prefix + "-storage-adapter",
        null)
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
}
