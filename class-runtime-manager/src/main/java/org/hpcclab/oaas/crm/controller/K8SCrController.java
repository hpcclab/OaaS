package org.hpcclab.oaas.crm.controller;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.json.Json;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.exception.CrDeployException;
import org.hpcclab.oaas.crm.exception.CrUpdateException;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.crm.template.CrTemplate;
import org.hpcclab.oaas.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

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
  final CrTemplate template;
  final KubernetesClient kubernetesClient;
  final OprcEnvironment.Config envConfig;
  final Map<String, CrComponentController<HasMetadata>> componentControllers;
  final Map<String, ProtoOClass> attachedCls = Maps.mutable.empty();
  final Map<String, ProtoOFunction> attachedFn = Maps.mutable.empty();
  final Map<String, FnCrComponentController<HasMetadata>> fnControllers = Maps.mutable.empty();
  final FnCrControllerFactory<HasMetadata> factory;
  final String namespace;
  CrDeploymentPlan currentPlan;
  boolean deleted = false;
  boolean initialized = false;


  public K8SCrController(CrTemplate template,
                         KubernetesClient client,
                         Map<String, CrComponentController<HasMetadata>> componentControllers,
                         FnCrControllerFactory<HasMetadata> factory,
                         OprcEnvironment.Config envConfig,
                         Tsid tsid) {
    this.template = template;
    this.kubernetesClient = client;
    this.envConfig = envConfig;
    this.namespace = envConfig.namespace();
    this.id = tsid.toLong();
    this.prefix = "cr-" + tsid.toLowerCase() + "-";
    this.factory = factory;
    this.componentControllers = componentControllers;
    for (CrComponentController<HasMetadata> componentController : componentControllers.values()) {
      componentController.init(this);
    }
  }

  public K8SCrController(CrTemplate template,
                         KubernetesClient client,
                         Map<String, CrComponentController<HasMetadata>> componentControllers,
                         FnCrControllerFactory<HasMetadata> factory,
                         OprcEnvironment.Config envConfig,
                         ProtoCr protoCr) {
    this(template, client,
      componentControllers,
      factory,
      envConfig,
      Tsid.from(protoCr.getId())
    );
    for (ProtoOClass protoOClass : protoCr.getAttachedClsList()) {
      attachedCls.put(protoOClass.getKey(), protoOClass);
    }
    for (ProtoOFunction function : protoCr.getAttachedFnList()) {
      attachedFn.put(function.getKey(), function);
      FnCrComponentController<HasMetadata> controller = factory.create(function);
      controller.init(this);
      fnControllers.put(function.getKey(), controller);
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
  public CrTemplate getTemplate() {
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
    ApplyK8SCrOperation crOperation = new ApplyK8SCrOperation(
      kubernetesClient,
      resourceList,
      () -> {
        attachedCls.put(unit.getCls().getKey(), unit.getCls());
        for (ProtoOFunction protoOFunction : unit.getFnListList()) {
          attachedFn.put(protoOFunction.getKey(), protoOFunction);
        }
        currentPlan = plan;
        componentControllers.values()
          .forEach(CrComponentController::updateStableTime);
        for (var controller : fnControllers.values()) {
          controller.updateStableTime();
        }
        initialized = true;
      });

    for (var componentController : componentControllers.values()) {
      resourceList.addAll(componentController.createDeployOperation(plan));
    }
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
    for (CrComponentController<HasMetadata> componentController : componentControllers.values()) {
      toDeleteResource.addAll(componentController.createDeleteOperation());
    }
    for (var controller : fnControllers.values()) {
      toDeleteResource.addAll(controller.createDeleteOperation());
    }
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
    for (CrComponentController<HasMetadata> componentController : componentControllers.values()) {
      resource.addAll(componentController.createAdjustOperation(adjustmentPlan));
    }
    var crOperation = new AdjustmentCrOperation(
      kubernetesClient,
      resource,
      () -> currentPlan = currentPlan.update(adjustmentPlan)
    );
    for (var entry : adjustmentPlan.fnInstances().entrySet()) {
      if (!fnControllers.containsKey(entry.getKey()))
        continue;
      FnCrComponentController<HasMetadata> controller = fnControllers.get(entry.getKey());
      resource.addAll(controller.createAdjustOperation(adjustmentPlan));
      OFunctionStatusUpdate update = controller.buildStatusUpdate();
      if (update!=null)
        crOperation.getFnUpdates().add(update);
    }
    return crOperation;
  }

  @Override
  public CrDeploymentPlan currentPlan() {
    return currentPlan;
  }

  protected FnResourcePlan deployFunction(CrDeploymentPlan newPlan,
                                          ProtoOFunction function) throws CrDeployException {
    FnCrComponentController<HasMetadata> fnController = factory.create(function);
    fnController.init(this);
    fnControllers.put(function.getKey(), fnController);
    List<HasMetadata> resources = fnController.createDeployOperation(newPlan);
    OFunctionStatusUpdate update = fnController.buildStatusUpdate();
    if (update != null)
      return new FnResourcePlan(resources, List.of(update));
    else
      return new FnResourcePlan(resources, List.of());
  }

  protected List<HasMetadata> removeFunction(String fnKey) throws CrUpdateException {
    if (fnControllers.containsKey(fnKey)) {
      CrComponentController<HasMetadata> controller = fnControllers.get(fnKey);
      return controller.createDeleteOperation();
    }
    return List.of();
  }

  @Override
  public ProtoCr dump() {
    var str = Json.encode(currentPlan);
    return ProtoCr.newBuilder()
      .setId(id)
      .setTemplate(template.name())
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

  @Override
  public long getStableTime(String name) {
    if (componentControllers.containsKey(name))
      return componentControllers.get(name).getStableTime();
    if (fnControllers.containsKey(name))
      return fnControllers.get(name).getStableTime();
    return -1;
  }
}
