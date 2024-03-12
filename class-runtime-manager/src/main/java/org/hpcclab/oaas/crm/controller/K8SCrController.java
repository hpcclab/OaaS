package org.hpcclab.oaas.crm.controller;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.exception.CrUpdateException;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDataSpec;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;
import org.hpcclab.oaas.crm.template.ClassRuntimeTemplate;
import org.hpcclab.oaas.proto.*;
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
  public static final String NAME_FUNCTION = "function";
  public static final String NAME_CONFIGMAP = "cm";
  private static final Logger logger = LoggerFactory.getLogger(K8SCrController.class);
  final long id;
  final String prefix;
  final String namespace;
  final ClassRuntimeTemplate template;
  final KubernetesClient kubernetesClient;
  final OprcEnvironment.Config envConfig;
  final CrComponentController<HasMetadata> invokerController;
  final CrComponentController<HasMetadata> saController;
  final CrComponentController<HasMetadata> configController;
  final Map<String, ProtoOClass> attachedCls = Maps.mutable.empty();
  final Map<String, ProtoOFunction> attachedFn = Maps.mutable.empty();
  final CrFnController<HasMetadata> deploymentFnController;
  final CrFnController<HasMetadata> knativeFnController;
  CrDeploymentPlan currentPlan;
  CrDataSpec dataSpec;
  boolean deleted = false;
  boolean initialized = false;


  public K8SCrController(ClassRuntimeTemplate template,
                         KubernetesClient client,
                         CrComponentController<HasMetadata> invokerController,
                         CrComponentController<HasMetadata> saController,
                         CrComponentController<HasMetadata> configController,
                         CrFnController<HasMetadata> deploymentFnController,
                         CrFnController<HasMetadata> knativeFnController,
                         OprcEnvironment.Config envConfig,
                         Tsid tsid) {
    this.template = template;
    this.kubernetesClient = client;
    this.envConfig = envConfig;
    namespace = kubernetesClient.getNamespace();
    id = tsid.toLong();
    prefix = "cr-" + tsid.toLowerCase() + "-";
    this.deploymentFnController = deploymentFnController;
    deploymentFnController.init(this);
    this.knativeFnController = knativeFnController;
    knativeFnController.init(this);
    this.invokerController = invokerController;
    this.invokerController.init(this);
    this.saController = saController;
    this.saController.init(this);
    this.configController = configController;
    this.configController.init(this);
  }

  public K8SCrController(ClassRuntimeTemplate template,
                         KubernetesClient client,
                         CrComponentController<HasMetadata> invokerController,
                         CrComponentController<HasMetadata> saController,
                         CrComponentController<HasMetadata> configController,
                         CrFnController<HasMetadata> deploymentFnController,
                         CrFnController<HasMetadata> knativeFnController,
                         OprcEnvironment.Config envConfig,
                         ProtoCr protoCr) {
    this(template, client,
      invokerController,
      saController,
      configController,
      deploymentFnController,
      knativeFnController,
      envConfig,
      Tsid.from(protoCr.getId())
    );
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
        currentPlan = plan;
        invokerController.updateStabilizationTime();
        saController.updateStabilizationTime();
        plan.fnInstances().keySet()
          .forEach(deploymentFnController::updateStabilizationTime);
        plan.fnInstances().keySet()
          .forEach(knativeFnController::updateStabilizationTime);
        initialized = true;
      });
    resourceList.addAll(configController.createDeployOperation(null));
    resourceList.addAll(saController.createDeployOperation(plan.coreInstances().get(STORAGE_ADAPTER)));
    resourceList.addAll(invokerController.createDeployOperation(plan.coreInstances().get(INVOKER)));
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
    });
  }

  @Override
  public CrOperation createDestroyOperation() throws CrUpdateException {
    List<HasMetadata> toDeleteResource = Lists.mutable.empty();
    toDeleteResource.addAll(invokerController.createDeleteOperation());
    toDeleteResource.addAll(saController.createDeleteOperation());
    toDeleteResource.addAll(configController.createDeleteOperation());
    var fn = deploymentFnController.removeAllFunction();
    toDeleteResource.addAll(fn);
    var ksvc = knativeFnController.removeAllFunction();
    toDeleteResource.addAll(ksvc);
    return new DeleteK8SCrOperation(kubernetesClient, toDeleteResource,
      () -> {
        attachedCls.clear();
        attachedFn.clear();
        deleted = true;
      });
  }

  @Override
  public CrOperation createAdjustmentOperation(CrAdjustmentPlan adjustmentPlan) {
    List<HasMetadata> resource = Lists.mutable.empty();
    resource.addAll(invokerController.createAdjustOperation(adjustmentPlan.coreInstances().get(INVOKER)));
    resource.addAll(saController.createAdjustOperation(adjustmentPlan.coreInstances().get(STORAGE_ADAPTER)));
    var fnResourcePlan = knativeFnController.applyAdjustment(adjustmentPlan);
    resource.addAll(fnResourcePlan.resources());
    var fnResourcePlan2 = deploymentFnController.applyAdjustment(adjustmentPlan);
    resource.addAll(fnResourcePlan2.resources());
    var crOperation = new AdjustmentCrOperation(
      kubernetesClient,
      resource,
      () -> currentPlan = currentPlan.update(adjustmentPlan)
    );
    crOperation.getFnUpdates().addAll(fnResourcePlan.fnUpdates());
    crOperation.getFnUpdates().addAll(fnResourcePlan2.fnUpdates());

    return crOperation;
  }

  @Override
  public CrDeploymentPlan currentPlan() {
    return currentPlan;
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

  @Override
  public boolean isDeleted() {
    return deleted;
  }
  @Override
  public boolean isInitialized() {
    return initialized;
  }
}
