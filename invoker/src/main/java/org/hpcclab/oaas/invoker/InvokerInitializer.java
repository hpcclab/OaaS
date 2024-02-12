package org.hpcclab.oaas.invoker;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnClsRepository;
import org.hpcclab.oaas.invoker.ispn.repo.EIspnFnRepository;
import org.hpcclab.oaas.invoker.mq.ClassListener;
import org.hpcclab.oaas.invoker.mq.FunctionListener;
import org.hpcclab.oaas.proto.ClassServiceGrpc;
import org.hpcclab.oaas.proto.CrStateServiceGrpc;
import org.hpcclab.oaas.proto.SingleKeyQuery;
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
  final VerticleDeployer verticleDeployer;
  final EIspnClsRepository clsRepo;
  final EIspnFnRepository fnRepo;
  final ClassControllerRegistry registry;
  @GrpcClient("package-manager")
  CrStateServiceGrpc.CrStateServiceBlockingStub crStateService;
  @GrpcClient("package-manager")
  ClassServiceGrpc.ClassServiceBlockingStub classService;

  @Inject
  public InvokerInitializer(InvokerConfig config,
                            ClassListener clsListener,
                            FunctionListener functionListener,
                            VerticleDeployer verticleDeployer,
                            EIspnClsRepository clsRepo,
                            EIspnFnRepository fnRepo,
                            ClassControllerRegistry registry) {
    this.config = config;
    this.clsListener = clsListener;
    this.functionListener = functionListener;
    this.verticleDeployer = verticleDeployer;
    this.clsRepo = clsRepo;
    this.fnRepo = fnRepo;
      this.registry = registry;
  }

  void init(@Observes StartupEvent event) {
    loadAssignedCls();
    clsListener.setHandler(cls -> {
      logger.info("receive cls[{}] update event", cls.getKey());
      clsRepo.getCache().putForExternalRead(cls.getKey(), cls);
      registry.registerOrUpdate(cls)
        .await().indefinitely();
    });
    clsListener.start().await().indefinitely();
    functionListener.setHandler(fn -> {
      logger.info("receive fn[{}] update event", fn.getKey());
      fnRepo.getCache().putForExternalRead(fn.getKey(), fn);
      registry.updateFunction(fn);
    });
    clsListener.start().await().indefinitely();
  }

  public void loadAssignedCls() {
    List<String> clsList = List.of();
    if (config.loadMode()==InvokerConfig.LoadAssignMode.FETCH) {
      var crId = ConfigProvider.getConfig()
        .getValue("oprc.crid", String.class);
      logger.info("loading CR [id={}]", crId);
      var orbit = crStateService.get(SingleKeyQuery.newBuilder().setKey(crId).build());
      logger.info("handle CR [id={}, cls={}, fn={}]",
        orbit.getId(), orbit.getAttachedClsList(), orbit.getAttachedFnList());
      clsList = orbit.getAttachedClsList();

    } else if (config.loadMode() == InvokerConfig.LoadAssignMode.ENV){
      clsList = config.initClass();
      if (clsList.getFirst().equals("none")) clsList = List.of();
    }
    for (var clsKey : clsList) {
      var cls = classService.get(SingleKeyQuery.newBuilder().setKey(clsKey).build());
      registry.registerOrUpdate(cls)
        .await().indefinitely();
      verticleDeployer.handleCls(cls);
    }
    if (logger.isInfoEnabled())
      logger.info("setup class controller registry:\n{}", registry.printStructure());
  }
}
