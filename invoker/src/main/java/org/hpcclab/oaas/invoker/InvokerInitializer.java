package org.hpcclab.oaas.invoker;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.invoker.lookup.HashRegistry;
import org.hpcclab.oaas.invoker.mq.ClassListener;
import org.hpcclab.oaas.invoker.mq.CrHashListener;
import org.hpcclab.oaas.invoker.mq.FunctionListener;
import org.hpcclab.oaas.proto.*;
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
  final HashRegistry hashRegistry;
  final InvokerManager invokerManager;
  final CrStateService crStateService;
  final ClassService classService;


  @Inject
  public InvokerInitializer(InvokerConfig config,
                            ClassListener clsListener,
                            FunctionListener functionListener,
                            CrHashListener hashListener,
                            HashRegistry hashRegistry,
                            InvokerManager invokerManager,
                            @GrpcClient("package-manager")
                            CrStateService crStateService,
                            @GrpcClient("package-manager")
                            ClassService classService) {
    this.config = config;
    this.clsListener = clsListener;
    this.functionListener = functionListener;
    this.hashListener = hashListener;
    this.hashRegistry = hashRegistry;
    this.invokerManager = invokerManager;
    this.crStateService = crStateService;
    this.classService = classService;
  }

  void init(@Observes StartupEvent event) throws Throwable {
    VertxContextSupport.subscribeAndAwait(() ->
      Vertx.currentContext().executeBlocking(() -> {
        functionListener.setHandler(fn -> {
            logger.info("receive fn[{}] update event", fn.getKey());
            invokerManager.update(fn)
              .subscribe().with(v -> {
              });
          })
          .start().await().indefinitely();
        clsListener.setHandler(cls -> {
            logger.info("receive cls[{}] update event", cls.getKey());
            invokerManager.update(cls)
              .subscribe().with(v -> {
              });
          })
          .start().await().indefinitely();
        hashListener.setHandler(protoCrHash -> {
            if (!invokerManager.getManagedCls().contains(protoCrHash.getCls()))
              hashRegistry.updateManaged(protoCrHash);
          })
          .start().await().indefinitely();
        loadAssignedCls();
        if (config.warmHashCache())
          hashRegistry.warmCache().await().indefinitely();
        return null;
      }));
  }

  public void loadAssignedCls() {
    List<ProtoOClass> clsList = List.of();
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
      var clsKeyList = config.initClass();
      if (clsKeyList.getFirst().equals("none")) {
        clsList = List.of();
      } else {
        clsList = classService.select(MultiKeyQuery.newBuilder().addAllKey(clsKeyList).build())
          .collect().asList().await().indefinitely();
      }
    }
    Multi.createFrom().iterable(clsList)
      .call(invokerManager::registerManaged)
      .collect().last()
      .await().indefinitely();
    if (config.loadMode()==InvokerConfig.LoadAssignMode.FETCH) {
      classService.list(PaginateQuery.newBuilder().setLimit(1000).build())
        .call(invokerManager::update)
        .collect().last()
        .await().indefinitely();
    }
    if (logger.isInfoEnabled())
      logger.info("setup class controller registry:\n{}",
        invokerManager.getRegistry().printStructure());
  }
}
