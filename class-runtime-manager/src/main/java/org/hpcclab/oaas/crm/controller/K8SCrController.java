package org.hpcclab.oaas.crm.controller;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.exception.CrUpdateException;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.crm.template.ClassRuntimeTemplate;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class K8SCrController implements CrController {
  private static final Logger logger = LoggerFactory.getLogger(K8SCrController.class);
  final long id;
  final String prefix;
  final String namespace;
  final ClassRuntimeTemplate template;
  final KubernetesClient kubernetesClient;
  final OprcEnvironment.Config envConfig;
  Map<String, ProtoOClass> attachedCls = Maps.mutable.empty();
  Map<String, ProtoOFunction> attachedFn = Maps.mutable.empty();
  Map<String, Long> versions = Maps.mutable.empty();
  List<HasMetadata> k8sResources = Lists.mutable.empty();
  DeploymentFnController deploymentFnController;
  KnativeFnController knativeFnController;
  CrDeploymentPlan currentPlan;
  boolean isDeleted = false;


  public static final String CR_LABEL_KEY = "cr-id";
  public static final String CR_COMPONENT_LABEL_KEY = "cr-part";
  public static final String CR_FN_KEY = "cr-fn";
  public static final String NAME_INVOKER = "invoker";
  public static final String NAME_SA = "storage-adapter";
  public static final String NAME_SECRET = "secret";
  public static final String NAME_CONFIGMAP = "cm";

  public K8SCrController(ClassRuntimeTemplate template,
                         KubernetesClient client,
                         OprcEnvironment.Config envConfig,
                         Tsid tsid) {
    this.template = template;
    this.kubernetesClient = client;
    this.envConfig = envConfig;
    namespace = kubernetesClient.getNamespace();
    id = tsid.toLong();
    prefix = "cr-" + tsid.toLowerCase() + "-";
    deploymentFnController = new DeploymentFnController(kubernetesClient, this);
    knativeFnController = new KnativeFnController(kubernetesClient, this, envConfig);
  }

  public K8SCrController(ClassRuntimeTemplate template,
                         KubernetesClient client,
                         OprcEnvironment.Config envConfig,
                         ProtoCr protoCr) {
    this(template, client, envConfig, Tsid.from(protoCr.getId()));
    for (ProtoOClass protoOClass : protoCr.getAttachedClsList()) {
      attachedCls.put(protoOClass.getKey(), protoOClass);
    }
    for (ProtoOFunction protoOFunction : protoCr.getAttachedFnList()) {
      attachedFn.put(protoOFunction.getKey(), protoOFunction);
    }
    var jsonDump = protoCr.getState().getJsonDump();
    if (!jsonDump.isEmpty()) {
      currentPlan = Json.decodeValue(jsonDump, CrDeploymentPlan.class);
    }
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public Map<String, ProtoOClass> getAttachedCls() {
    return attachedCls;
  }

  @Override
  public Map<String, ProtoOFunction> getAttachedFn() {
    return attachedFn;
  }

  @Override
  public CrOperation createUpdateOperation(CrDeploymentPlan plan, DeploymentUnit unit) {
    List<HasMetadata> resources = Lists.mutable.empty();
    ApplyK8SCrOperation crOperation = new ApplyK8SCrOperation(kubernetesClient, resources, () -> {
      for (var f : unit.getFnListList()) {
        attachedFn.put(f.getKey(), f);
      }
      k8sResources.addAll(resources);
      currentPlan = plan;
    });

    for (var f : unit.getFnListList()) {
      FnResourcePlan fnResourcePlan = deployFunction(plan, f);
      resources.addAll(fnResourcePlan.resources());
      crOperation.getFnUpdates().addAll(fnResourcePlan.fnUpdates());
    }
    return crOperation;
  }

  @Override
  public CrDeploymentPlan createDeploymentPlan(DeploymentUnit unit) {
    return template.getQosOptimizer().resolve(unit);
  }

  @Override
  public CrDeploymentPlan currentPlan() {
    return currentPlan;
  }

  @Override
  public CrOperation createDeployOperation(CrDeploymentPlan plan, DeploymentUnit unit)
    throws CrDeployException {
    List<HasMetadata> resourceList = Lists.mutable.empty();
    ApplyK8SCrOperation crOperation = new ApplyK8SCrOperation(kubernetesClient, resourceList,
      () -> {
        attachedCls.put(unit.getCls().getKey(), unit.getCls());
        for (ProtoOFunction protoOFunction : unit.getFnListList()) {
          attachedFn.put(protoOFunction.getKey(), protoOFunction);
        }
        k8sResources.addAll(resourceList);
        currentPlan = plan;
      });
    resourceList.addAll(deployShared(plan));
    resourceList.addAll(deployDataModule(plan));
    resourceList.addAll(deployExecutionModule(plan));
    resourceList.addAll(deployObjectModule(plan, unit));
    for (ProtoOFunction fn : unit.getFnListList()) {
      var fnResourcePlan = deployFunction(plan, fn);
      resourceList.addAll(fnResourcePlan.resources());
      crOperation.getFnUpdates().addAll(fnResourcePlan.fnUpdates());
    }
    return crOperation;
  }


  public List<HasMetadata> deployShared(CrDeploymentPlan plan) throws CrDeployException {
    var labels = Map.of(
      CR_LABEL_KEY, String.valueOf(id)
    );
    var datastoreMap = DatastoreConfRegistry.getDefault().dump();
    var sec = new SecretBuilder()
      .withNewMetadata()
      .withName(prefix + NAME_SECRET)
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withStringData(datastoreMap)
      .build();

    var confMapData = Map.of(
      "OPRC_INVOKER_KAFKA", envConfig.kafkaBootstrap(),
      "OPRC_INVOKER_SA_URL", "http://%sstorage-adapter.%s.svc.cluster.local"
        .formatted(prefix, namespace),
      "OPRC_CRID", Tsid.from(id).toLowerCase(),
      "OPRC_INVOKER_PMHOST", envConfig.classManagerHost(),
      "OPRC_INVOKER_PMPORT", envConfig.classManagerPort()
    );
    var confMap = new ConfigMapBuilder()
      .withNewMetadata()
      .withName(prefix + NAME_CONFIGMAP)
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withData(confMapData)
      .build();

    return List.of(confMap, sec);
  }

  public List<HasMetadata> deployObjectModule(CrDeploymentPlan plan, DeploymentUnit unit) {
    var labels = Map.of(
      CR_LABEL_KEY, String.valueOf(id),
      CR_COMPONENT_LABEL_KEY, NAME_INVOKER
    );
    var deployment = createDeployment(
      "/crts/invoker-dep.yml",
      prefix + NAME_INVOKER,
      NAME_INVOKER,
      labels);
    deployment.getSpec().setReplicas(plan.coreInstances().get(OprcComponent.INVOKER));
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var invokerSvc = createSvc(
      "/crts/invoker-svc.yml",
      prefix + NAME_INVOKER,
      labels);
    var invokerSvcPing = createSvc(
      "/crts/invoker-svc-ping.yml",
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
      CR_LABEL_KEY, String.valueOf(id),
      CR_COMPONENT_LABEL_KEY, NAME_SA
    );
    var deployment = createDeployment(
      "/crts/storage-adapter-dep.yml",
      prefix + NAME_SA,
      NAME_SA,
      labels);
    deployment.getSpec().setReplicas(plan.coreInstances().get(OprcComponent.STORAGE_ADAPTER));
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var svc = createSvc(
      "/crts/storage-adapter-svc.yml",
      prefix + NAME_SA,
      labels);
    return List.of(deployment, svc);
  }

  public FnResourcePlan deployFunction(CrDeploymentPlan plan,
                                       ProtoOFunction function) throws CrDeployException {
    if (function.getType()==ProtoFunctionType.PROTO_FUNCTION_TYPE_MACRO)
      return FnResourcePlan.EMPTY;
    if (function.getType()==ProtoFunctionType.PROTO_FUNCTION_TYPE_LOGICAL)
      return FnResourcePlan.EMPTY;
    if (!function.getProvision().getDeployment().getImage().isEmpty()) {
      return deploymentFnController.deployFunction(plan, function);
    } else if (!function.getProvision().getKnative().getImage().isEmpty()) {
      return knativeFnController.deployFunction(plan, function);
    }
    throw new CrDeployException("Can not find suitable function controller for function:\n" + function);
  }

  public List<HasMetadata> removeFunction(String fnKey) throws CrUpdateException {
    List<HasMetadata> resourceList = Lists.mutable.empty();
    resourceList.addAll(deploymentFnController.removeFunction(fnKey));
    resourceList.addAll(knativeFnController.removeFunction(fnKey));
    return resourceList;
  }

  @Override
  public CrOperation createDetachOperation(ProtoOClass cls) throws CrUpdateException {
    // TODO send signal
    if (attachedCls.size()==1 && attachedCls.containsKey(cls.getKey())) {
      return createDestroyOperation();
    }
    List<HasMetadata> resourceList = Lists.mutable.empty();
    for (ProtoFunctionBinding fb : cls.getFunctionsList()) {
      resourceList.addAll(removeFunction(fb.getFunction()));
    }
    return new DeleteK8SCrOperation(kubernetesClient, resourceList, () -> {
      attachedCls.remove(cls.getKey());
      for (ProtoFunctionBinding fb : cls.getFunctionsList()) {
        attachedFn.remove(fb.getFunction());
      }
      k8sResources.removeAll(resourceList);
    });
  }

  @Override
  public CrOperation createDestroyOperation() throws CrUpdateException {
    if (k8sResources.isEmpty()) {
      var depList = kubernetesClient.apps().deployments()
        .withLabel(CR_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(depList);
      var svcList = kubernetesClient.services()
        .withLabel(CR_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(svcList);
      var sec = kubernetesClient.secrets()
        .withLabel(CR_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(sec);
      var configMaps = kubernetesClient.configMaps()
        .withLabel(CR_LABEL_KEY, String.valueOf(id))
        .list()
        .getItems();
      k8sResources.addAll(configMaps);
      var ksvc = knativeFnController.removeAllFunction();
      k8sResources.addAll(ksvc);
    }
    return new DeleteK8SCrOperation(kubernetesClient, k8sResources,
      () -> {
        k8sResources.clear();
        isDeleted = true;
      });
  }

  @Override
  public CrOperation createAdjustmentOperation(CrAdjustmentPlan adjustmentPlan) {
    List<HasMetadata> resource = Lists.mutable.empty();
    for (var entry : adjustmentPlan.coreInstances().entrySet()) {
      String name = switch (entry.getKey()) {
        case INVOKER -> prefix + NAME_INVOKER;
        case STORAGE_ADAPTER -> prefix + NAME_SA;
        default -> null;
      };
      if (name==null) continue;
      var deployment = kubernetesClient.apps().deployments()
        .inNamespace(namespace)
        .withName(name).get();
      if (deployment==null) continue;
      deployment.getSpec().setReplicas(entry.getValue());
      resource.add(deployment);
    }
    var crOperation = new ApplyK8SCrOperation(
      kubernetesClient,
      resource,
      () -> currentPlan = currentPlan.update(adjustmentPlan)
    );
    var fnResourcePlan = knativeFnController.applyAdjustment(adjustmentPlan);
    resource.addAll(fnResourcePlan.resources());
    crOperation.getFnUpdates().addAll(fnResourcePlan.fnUpdates());
    var fnResourcePlan2 = deploymentFnController.applyAdjustment(adjustmentPlan);
    resource.addAll(fnResourcePlan2.resources());
    crOperation.getFnUpdates().addAll(fnResourcePlan2.fnUpdates());

    return crOperation;
  }

  protected Deployment createDeployment(String filePath,
                                        String name,
                                        String configName,
                                        Map<String, String> labels) {
    var is = getClass().getResourceAsStream(filePath);
    var crtConfig = template.getConfig();
    var image = crtConfig.images().get(configName);
    var deployment = kubernetesClient.getKubernetesSerialization()
      .unmarshal(is, Deployment.class);
    Container container = deployment.getSpec()
      .getTemplate()
      .getSpec()
      .getContainers()
      .getFirst();
    container.setImage(image);
    var additionalEnv = template.getConfig().additionalEnv().get(configName);
    for (Map.Entry<String, String> entry : additionalEnv.entrySet()) {
      addEnv(container, entry.getKey(), entry.getValue());
    }
    rename(deployment, name);
    attachLabels(deployment, labels);
    return deployment;
  }

  @Override
  public ProtoCr dump() {
    var str = Json.encode(currentPlan);
    return ProtoCr.newBuilder()
      .setId(id)
      .setType(template.type())
      .setNamespace(namespace)
      .addAllAttachedCls(attachedCls.values())
      .addAllAttachedFn(attachedFn.values())
      .setState(ProtoCrState.newBuilder().setJsonDump(str).build())
      .build();
  }

  @Override
  public QosOptimizer getOptimizer() {
    return template.getQosOptimizer();
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

  @Override
  public boolean isDeleted() {
    return isDeleted;
  }
}
