package org.hpcclab.oprc.cli.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.LocationAwareInvocationForwarder;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.controller.CcInvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.fn.ChainFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.MacroFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.TaskFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.logical.*;
import org.hpcclab.oaas.invocation.dataflow.DataflowOrchestrator;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.HttpOffLoaderFactory;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;
import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oaas.storage.UnifyContentUrlGenerator;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;

import java.io.IOException;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class LocalInvocationEngineProvider {

  final ConfigFileManager fileManager;
  final FileCliConfig fileCliConfig;
  final LocalDevManager localStateManager;

  public LocalInvocationEngineProvider(ConfigFileManager fileManager, LocalDevManager localStateManager) throws IOException {
    this.fileManager = fileManager;
    this.fileCliConfig = fileManager.getOrCreate();
    this.localStateManager = localStateManager;
  }

  @Produces
  @ApplicationScoped
  ClassControllerRegistry controllerRegistry() {
    return localStateManager.getControllerRegistry();
  }

  @Produces
  @ApplicationScoped
  InvocationManager invocationManager(ClassControllerRegistry registry,
                                      InvocationReqHandler reqHandler) {
    return new InvocationManager(
      registry,
      localStateManager.getClassControllerBuilder(),
      reqHandler);
  }

  @Produces
  InvocationQueueProducer queueProducer() {
    return request -> Uni.createFrom().nullItem();
  }

  @Produces
  MetricFactory metricFactory() {
    return new MetricFactory.NoOpMetricFactory();
  }

  @Produces
  IdGenerator idGenerator() {
    return new TsidGenerator();
  }

  @Produces
  TaskFunctionController taskFunctionController(IdGenerator idGenerator,
                                                ObjectMapper mapper,
                                                OffLoaderFactory offLoaderFactory,
                                                ContentUrlGenerator contentUrlGenerator) {
    return new TaskFunctionController(idGenerator,
      mapper, offLoaderFactory, contentUrlGenerator);
  }

  @Produces
  MacroFunctionController macroFunctionController(IdGenerator idGenerator,
                                                  ObjectMapper mapper,
                                                  DataflowOrchestrator orchestrator) {
    return new MacroFunctionController(idGenerator, mapper, orchestrator);
  }

  @Produces
  ChainFunctionController chainFunctionController(IdGenerator idGenerator,
                                                  ObjectMapper mapper) {
    return new ChainFunctionController(idGenerator, mapper);
  }

  @Produces
  DataflowOrchestrator dataflowOrchestrator(LocationAwareInvocationForwarder invocationHandler,
                                            IdGenerator idGenerator) {
    return new DataflowOrchestrator(invocationHandler, idGenerator);
  }


  @Produces
  NewFnController newFunctionController(IdGenerator idGenerator,
                                        ObjectMapper mapper,
                                        ContentUrlGenerator contentUrlGenerator) {
    return new NewFnController(idGenerator, mapper, contentUrlGenerator);
  }

  @Produces
  CopyFnController copyFunctionController(IdGenerator idGenerator,
                                          ObjectMapper mapper) {
    return new CopyFnController(idGenerator, mapper);
  }

  @Produces
  UpdateFnController updateFunctionController(IdGenerator idGenerator,
                                              ObjectMapper mapper) {
    return new UpdateFnController(idGenerator, mapper);
  }

  @Produces
  GetFnController getFnController(IdGenerator idGenerator,
                                  ObjectMapper mapper) {
    return new GetFnController(idGenerator, mapper);
  }

  @Produces
  ProjectFnController projectFnController(IdGenerator idGenerator,
                                          ObjectMapper mapper) {
    return new ProjectFnController(idGenerator, mapper);
  }

  @Produces
  FileFnController fileFnController(IdGenerator idGenerator,
                                    ObjectMapper mapper,
                                    ContentUrlGenerator generator,
                                    ObjectMapper objectMapper) {
    return new FileFnController(idGenerator, mapper, generator, objectMapper);
  }



  @Produces
  @ApplicationScoped
  ContentUrlGenerator contentUrlGenerator() {
    FileCliConfig.LocalDevelopment localDev = fileCliConfig.getLocalDev();
    DatastoreConf datastoreConf = localDev.dataConf();
    return new UnifyContentUrlGenerator(
      "http://" + localDev.localhost() + ":" + localDev.port(),
      datastoreConf
    );
  }

  @Produces
  @ApplicationScoped
  LocationAwareInvocationForwarder forwarder(InvocationReqHandler reqHandler) {
    return reqHandler::invoke;
  }

  @Produces
  @ApplicationScoped
  InvocationReqHandler invocationReqHandler(ClassControllerRegistry classControllerRegistry,
                                            IdGenerator idGenerator) {
    return new CcInvocationReqHandler(classControllerRegistry,
      localStateManager.getCtxLoader(),
      idGenerator,
      100
    );
  }



  @ApplicationScoped
  @Produces
  HttpOffLoaderFactory factory(Vertx vertx) {
    HttpOffLoaderConfig config = HttpOffLoaderConfig.builder()
      .appName("oparaca/cli")
      .build();
    return new HttpOffLoaderFactory(vertx, config);
  }


}
