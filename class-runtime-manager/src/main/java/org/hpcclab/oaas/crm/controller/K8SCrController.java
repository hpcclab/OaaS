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
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;
import org.hpcclab.oaas.crm.template.ClassRuntimeTemplate;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hpcclab.oaas.crm.OprcComponent.INVOKER;
import static org.hpcclab.oaas.crm.OprcComponent.STORAGE_ADAPTER;

public class K8SCrController implements CrController {
  public static final String CR_LABEL_KEY = "cr-id";
  public static final String CR_COMPONENT_LABEL_KEY = "cr-part";
  public static final String CR_FN_KEY = "cr-fn";
  public static final String NAME_SECRET = "secret";
  public static final String NAME_CONFIGMAP = "cm";
  private static final Logger logger = LoggerFactory.getLogger(K8SCrController.class);
  final long id;
  final String prefix;
  final String namespace;
  final ClassRuntimeTemplate template;
  final KubernetesClient kubernetesClient;
  final OprcEnvironment.Config envConfig;
  Map<String, ProtoOClass> attachedCls = Maps.mutable.empty();
  Map<String, ProtoOFunction> attachedFn = Maps.mutable.empty();
  List<HasMetadata> k8sResources = Lists.mutable.empty();
  DeploymentFnController deploymentFnController;
  KnativeFnController knativeFnController;
  CrDeploymentPlan currentPlan;
  boolean deleted = false;
  boolean initialized = false;
  Map<String, Long> stabilizationTimeMap = Maps.mutable.empty();


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
    deleted = protoCr.getDeleted();
    initialized = true;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public ClassRuntimeTemplate getTemplate() {
    return template;
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
        plan.coreInstances().keySet()
          .forEach(this::updateStabilizationTime);
        plan.fnInstances().keySet()
          .forEach(this::updateStabilizationTime);
        initialized = true;
      });
    resourceList.addAll(deployShared(plan));
    resourceList.addAll(deployDataModule(plan));
    resourceList.addAll(deployExecutionModule(plan));
    resourceList.addAll(deployObjectModule(plan, unit));
    crOperation.getClsUpdates().add(OClassStatusUpdate.newBuilder()
      .setKey(unit.getCls().getKey())
      .setStatus(ProtoOClassDeploymentStatus.newBuilder()
        .setCrId(getId())
        .build())
      .build());
    for (ProtoOFunction fn : unit.getFnListList()) {
      var fnResourcePlan = deployFunction(plan, fn);
      resourceList.addAll(fnResourcePlan.resources());
      crOperation.getFnUpdates().addAll(fnResourcePlan.fnUpdates());
    }
    return crOperation;
  }


  @Override
  public CrOperation createUpdateOperation(CrDeploymentPlan plan, DeploymentUnit unit) {
    List<HasMetadata> resources = Lists.mutable.empty();
    ApplyK8SCrOperation crOperation = new ApplyK8SCrOperation(kubernetesClient, resources, () -> {
      attachedCls.put(unit.getCls().getKey(), unit.getCls());
      for (var f : unit.getFnListList()) {
        attachedFn.put(f.getKey(), f);
      }
      k8sResources.addAll(resources);
      currentPlan = plan;
    });
    crOperation.getClsUpdates().add(OClassStatusUpdate.newBuilder()
      .setKey(unit.getCls().getKey())
      .setStatus(ProtoOClassDeploymentStatus.newBuilder()
        .setCrId(getId())
        .build())
      .build());

    for (var f : unit.getFnListList()) {
      var oldFunc = attachedFn.get(f.getKey());
      if (oldFunc!=null && oldFunc.equals(f))
        continue;
      FnResourcePlan fnResourcePlan = deployFunction(plan, f);
      resources.addAll(fnResourcePlan.resources());
      crOperation.getFnUpdates().addAll(fnResourcePlan.fnUpdates());
    }
    return crOperation;
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
    List<HasMetadata> toDeleteResource = Lists.mutable.empty();
    String tsidString = getTsidString();
    var depList = kubernetesClient.apps().deployments()
      .withLabel(CR_LABEL_KEY, tsidString)
      .list()
      .getItems();
    toDeleteResource.addAll(depList);
    var svcList = kubernetesClient.services()
      .withLabel(CR_LABEL_KEY, tsidString)
      .list()
      .getItems();
    toDeleteResource.addAll(svcList);
    var sec = kubernetesClient.secrets()
      .withLabel(CR_LABEL_KEY, tsidString)
      .list()
      .getItems();
    toDeleteResource.addAll(sec);
    var configMaps = kubernetesClient.configMaps()
      .withLabel(CR_LABEL_KEY, tsidString)
      .list()
      .getItems();
    toDeleteResource.addAll(configMaps);
    var ksvc = knativeFnController.removeAllFunction();
    toDeleteResource.addAll(ksvc);
    var podMonitor = kubernetesClient.genericKubernetesResources("monitoring.coreos.com/v1", "PodMonitor")
      .withLabel(CR_LABEL_KEY, tsidString)
      .list()
      .getItems();
    toDeleteResource.addAll(podMonitor);

    return new DeleteK8SCrOperation(kubernetesClient, toDeleteResource,
      () -> {
        k8sResources.clear();
        attachedCls.clear();
        attachedFn.clear();
        deleted = true;
      });
  }

  protected CrAdjustmentPlan filterNonStable(CrAdjustmentPlan adjustmentPlan) {
    long currentTimeMillis = System.currentTimeMillis();

    Map<String, CrInstanceSpec> fnInstanceMap = adjustmentPlan.fnInstances()
      .entrySet()
      .stream()
      .filter(e -> stabilizationTimeMap.getOrDefault(e.getKey(), 0L) < currentTimeMillis)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    Map<OprcComponent, CrInstanceSpec> coreInstanceMap = adjustmentPlan.coreInstances()
      .entrySet()
      .stream()
      .filter(e -> stabilizationTimeMap.getOrDefault(e.getKey().getSvc(), 0L) < currentTimeMillis)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return adjustmentPlan
      .toBuilder()
      .fnInstances(fnInstanceMap)
      .coreInstances(coreInstanceMap)
      .build();
  }

  @Override
  public CrOperation createAdjustmentOperation(CrAdjustmentPlan adjustmentPlan) {
    CrAdjustmentPlan filtered = filterNonStable(adjustmentPlan);
    logger.debug("filter {} to {}", adjustmentPlan, filtered);
    logger.debug("stabilization time {}", stabilizationTimeMap);
    List<HasMetadata> resource = Lists.mutable.empty();
    for (var entry : filtered.coreInstances().entrySet()) {
      String name = prefix + entry.getKey().getSvc();
      var deployment = kubernetesClient.apps().deployments()
        .inNamespace(namespace)
        .withName(name).get();
      if (deployment==null) continue;
      deployment.getSpec().setReplicas(entry.getValue().minInstance());
      resource.add(deployment);
    }
    var fnResourcePlan = knativeFnController.applyAdjustment(filtered);
    resource.addAll(fnResourcePlan.resources());
    var fnResourcePlan2 = deploymentFnController.applyAdjustment(filtered);
    resource.addAll(fnResourcePlan2.resources());
    var crOperation = new AdjustmentCrOperation(
      kubernetesClient,
      resource,
      () -> {
        currentPlan = currentPlan.update(filtered);
        filtered.coreInstances().keySet()
          .forEach(this::updateStabilizationTime);
        filtered.fnInstances().keySet()
          .forEach(this::updateStabilizationTime);
      }
    );
    crOperation.getFnUpdates().addAll(fnResourcePlan.fnUpdates());
    crOperation.getFnUpdates().addAll(fnResourcePlan2.fnUpdates());

    return crOperation;
  }

  @Override
  public CrDeploymentPlan currentPlan() {
    return currentPlan;
  }


  protected List<HasMetadata> deployShared(CrDeploymentPlan plan) throws CrDeployException {
    var labels = Map.of(
      CR_LABEL_KEY, getTsidString()
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

  protected List<HasMetadata> deployObjectModule(CrDeploymentPlan plan, DeploymentUnit unit) {
    var labels = Map.of(
      CR_LABEL_KEY, getTsidString(),
      CR_COMPONENT_LABEL_KEY, INVOKER.getSvc()
    );
    var deployment = createDeployment(
      "/crts/invoker-dep.yml",
      prefix + INVOKER.getSvc(),
      INVOKER.getSvc(),
      labels,
      plan.coreInstances().get(INVOKER)
    );
    var podMonitor = K8sResourceUtil
      .createPodMonitor(prefix + INVOKER.getSvc(), namespace, labels);
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var invokerSvc = createSvc(
      "/crts/invoker-svc.yml",
      prefix + INVOKER.getSvc(),
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
    return List.of(deployment, podMonitor, invokerSvc, invokerSvcPing);
  }

  protected List<HasMetadata> deployExecutionModule(CrDeploymentPlan plan) {
    // no needed
    return List.of();
  }

  protected List<HasMetadata> deployDataModule(CrDeploymentPlan plan) throws CrDeployException {
    var labels = Map.of(
      CR_LABEL_KEY, getTsidString(),
      CR_COMPONENT_LABEL_KEY, STORAGE_ADAPTER.getSvc()
    );
    var deployment = createDeployment(
      "/crts/storage-adapter-dep.yml",
      prefix + STORAGE_ADAPTER.getSvc(),
      STORAGE_ADAPTER.getSvc(),
      labels,
      plan.coreInstances().get(STORAGE_ADAPTER)
    );
    attachSecret(deployment, prefix + NAME_SECRET);
    attachConf(deployment, prefix + NAME_CONFIGMAP);
    var svc = createSvc(
      "/crts/storage-adapter-svc.yml",
      prefix + STORAGE_ADAPTER.getSvc(),
      labels);
    return List.of(deployment, svc);
  }

  protected FnResourcePlan deployFunction(CrDeploymentPlan plan,
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
    throw new CrDeployException("Can not find suitable functions controller for functions:\n" + function);
  }

  protected List<HasMetadata> removeFunction(String fnKey) throws CrUpdateException {
    List<HasMetadata> resourceList = Lists.mutable.empty();
    resourceList.addAll(deploymentFnController.removeFunction(fnKey));
    resourceList.addAll(knativeFnController.removeFunction(fnKey));
    return resourceList;
  }


  protected Deployment createDeployment(String filePath,
                                        String name,
                                        String configName,
                                        Map<String, String> labels,
                                        CrInstanceSpec spec) {
    var is = getClass().getResourceAsStream(filePath);
    var crtConfig = template.getConfig();
    var svc = crtConfig.services().get(configName);
    var image = svc.image();
    var deployment = kubernetesClient.getKubernetesSerialization()
      .unmarshal(is, Deployment.class);
    deployment.getSpec()
      .setReplicas(spec.minInstance());
    Container container = deployment.getSpec()
      .getTemplate()
      .getSpec()
      .getContainers()
      .getFirst();
    container.setImage(image);
    if (svc.imagePullPolicy()!=null && !svc.imagePullPolicy().isEmpty())
      container.setImagePullPolicy(svc.imagePullPolicy());
    container.setResources(K8sResourceUtil.makeResourceRequirements(spec));
    for (Map.Entry<String, String> entry : svc.env().entrySet()) {
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
      .setDeleted(deleted)
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

  @Override
  public boolean isDeleted() {
    return deleted;
  }


  protected void updateStabilizationTime(OprcComponent component) {
    logger.debug("updateStabilizationTime of {}", component.getSvc());
    stabilizationTimeMap.put(component.getSvc(),
      System.currentTimeMillis() +
        template.getConfig().services().get(component.getSvc()).stabilizationWindow()
    );
  }

  protected void updateStabilizationTime(String fnKey) {
    logger.debug("updateStabilizationTime of {}", fnKey);
    stabilizationTimeMap.put(fnKey,
      System.currentTimeMillis() + template.getConfig().functions().stabilizationWindow()
    );
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }
}
