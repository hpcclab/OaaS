package org.hpcclab.oaas.invoker;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.lookup.HashRegistry;
import org.hpcclab.oaas.invoker.mq.ClassListener;
import org.hpcclab.oaas.invoker.mq.CrHashListener;
import org.hpcclab.oaas.invoker.mq.FunctionListener;
import org.hpcclab.oaas.proto.ClassService;
import org.hpcclab.oaas.proto.CrStateService;
import org.hpcclab.oaas.proto.SingleKeyQuery;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class InvokerInitializer {
  private static final Logger logger = LoggerFactory.getLogger(InvokerInitializer.class);
  final InvokerConfig config;
  final ClassListener clsListener;
  final FunctionListener functionListener;
  final CrHashListener hashListener;
  final VerticleDeployer verticleDeployer;
  final ClassControllerRegistry registry;
  final HashRegistry hashRegistry;
  @GrpcClient("package-manager")
  CrStateService crStateService;
  @GrpcClient("package-manager")
  ClassService classService;

  @Inject
  public InvokerInitializer(InvokerConfig config,
                            ClassListener clsListener,
                            FunctionListener functionListener, CrHashListener hashListener,
                            VerticleDeployer verticleDeployer,
                            ClassControllerRegistry registry,
                            HashRegistry hashRegistry) {
    this.config = config;
    this.clsListener = clsListener;
    this.functionListener = functionListener;
      this.hashListener = hashListener;
      this.verticleDeployer = verticleDeployer;
    this.registry = registry;
    this.hashRegistry = hashRegistry;
  }

  void init(@Observes StartupEvent event) {
    loadAssignedCls();
    clsListener.setHandler(cls -> {
      logger.info("receive cls[{}] update event", cls.getKey());
      registry.registerOrUpdate(cls)
        .await().indefinitely();
    });
    clsListener.start().await().indefinitely();
    functionListener.setHandler(fn -> {
      logger.info("receive fn[{}] update event", fn.getKey());
      registry.updateFunction(fn);
    });
    clsListener.start().await().indefinitely();
    if (config.warmHashCache())
      hashRegistry.warmCache().await().indefinitely();
    hashListener.setHandler(hash -> hashRegistry.getMap().put(hash.getCls(), hash));
    hashListener.start().await().indefinitely();
  }

  public void loadAssignedCls() {
    List<String> clsList = List.of();
    if (config.loadMode()==InvokerConfig.LoadAssignMode.FETCH) {
      var crId = ConfigProvider.getConfig()
        .getValue("oprc.crid", String.class);
      logger.info("loading CR [id={}]", crId);
      var orbit = crStateService.get(SingleKeyQuery.newBuilder().setKey(crId).build())
        .await().indefinitely();
      logger.info("handle CR [id={}, cls={}, fn={}]",
        orbit.getId(), orbit.getAttachedClsList(), orbit.getAttachedFnList());
      clsList = orbit.getAttachedClsList();

    } else if (config.loadMode()==InvokerConfig.LoadAssignMode.ENV) {
      clsList = config.initClass();
      if (clsList.getFirst().equals("none")) clsList = List.of();
    }
    for (var clsKey : clsList) {
      var cls = classService.get(SingleKeyQuery.newBuilder().setKey(clsKey).build())
        .await().indefinitely();
      registry.registerOrUpdate(cls)
        .await().indefinitely();
      verticleDeployer.handleCls(cls);
    }
    if (logger.isInfoEnabled())
      logger.info("setup class controller registry:\n{}", registry.printStructure());
  }
}
