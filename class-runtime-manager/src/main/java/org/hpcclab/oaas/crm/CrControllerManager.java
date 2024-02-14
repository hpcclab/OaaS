package org.hpcclab.oaas.crm;

import com.github.f4b6a3.tsid.Tsid;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.template.CrTemplateManager;
import org.hpcclab.oaas.proto.*;
import org.hpcclab.oaas.proto.CrStateServiceGrpc.CrStateServiceBlockingStub;
import org.hpcclab.oaas.proto.InternalCrStateServiceGrpc.InternalCrStateServiceBlockingStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ApplicationScoped
public class CrControllerManager {
  private static final Logger logger = LoggerFactory.getLogger(CrControllerManager.class);
  final InternalCrStateServiceBlockingStub crStateUpdater;
  final CrStateServiceBlockingStub crStateService;
  final CrTemplateManager templateManager;
  final EnvironmentManager environmentManager;
  Map<Long, CrController> controllerMap = ConcurrentHashMap.newMap();

  @Inject
  public CrControllerManager(
    @GrpcClient("package-manager") InternalCrStateServiceBlockingStub crStateUpdater,
    @GrpcClient("package-manager") CrStateServiceBlockingStub crStateService,
    CrTemplateManager templateManager,
    EnvironmentManager environmentManager) {

    this.crStateUpdater = crStateUpdater;
    this.crStateService = crStateService;
    this.templateManager = templateManager;
    this.environmentManager = environmentManager;
  }

  public void loadAllToLocal() {
    var crs = crStateService.list(PaginateQuery.newBuilder().setLimit(1000).build());
    var env = environmentManager.getEnvironment();
    while (crs.hasNext()) {
      var protoCr = crs.next();
      var controller = templateManager.load(env, protoCr);
      controllerMap.put(controller.getId(), controller);
      if (logger.isInfoEnabled())
        logger.info("loaded CR to local: [{}, cls={}]",
          controller.getId(),
          controller.getAttachedCls()
        );
    }
  }

  public CrController get(long id) {
    return controllerMap.get(id);
  }

  public CrController getOrLoad(long id, OprcEnvironment env) {
    var orbit = controllerMap.get(id);
    if (orbit==null) {
      var protoCr = crStateService.get(SingleKeyQuery.newBuilder().setKey(Tsid.from(id).toLowerCase()).build());
      if (protoCr.getId()==0) return null;
      orbit = templateManager.load(env, protoCr);
      controllerMap.put(id, orbit);
    }
    return orbit;
  }
  public CrController getOrLoad(ProtoCr protoCr, OprcEnvironment env) {
    var orbit = controllerMap.get(protoCr.getId());
    if (orbit==null) {
      if (protoCr.getId()==0) return null;
      orbit = templateManager.load(env, protoCr);
      controllerMap.put(protoCr.getId(), orbit);
    }
    return orbit;
  }

  public CrController create(OprcEnvironment env,
                             DeploymentUnit deploymentUnit) {
    var template = templateManager.selectTemplate(env, deploymentUnit);
    var controller = template.create(env, deploymentUnit);
    saveToLocal(controller);
    return controller;
  }

  public void saveToLocal(CrController orbit) {
    controllerMap.put(orbit.getId(), orbit);
  }
  public void saveToRemote(CrController orbit) {
    saveToLocal(orbit);
    crStateUpdater.updateCr(orbit.dump());
  }

  public void deleteFromLocal(CrController controller) {
    controllerMap.remove(controller.getId());
  }
}
