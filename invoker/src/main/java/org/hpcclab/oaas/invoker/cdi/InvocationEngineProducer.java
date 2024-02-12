package org.hpcclab.oaas.invoker.cdi;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.ControllerInvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.CtxLoader;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.ispn.lookup.LookupManager;
import org.hpcclab.oaas.invoker.service.ControllerInvocationRecordHandler;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.invoker.service.InvocationRecordHandler;
import org.hpcclab.oaas.invoker.service.S3ContentUrlGenerator;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.mapper.ProtoObjectMapperImpl;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.hpcclab.oaas.repository.InvRepoManager;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InvocationEngineProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvocationEngineProducer.class);

  @Produces
  InvocationExecutor invocationGraphExecutor(
    InvocationQueueProducer sender,
    GraphStateManager graphStateManager,
    RepoContextLoader contextLoader,
    OffLoader offLoader,
    TaskFactory taskFactory,
    CompletedStateUpdater completionHandler) {
    return new InvocationExecutor(sender,
      graphStateManager,
      contextLoader,
      offLoader,
      taskFactory,
      completionHandler);
  }


  @Produces
  GraphStateManager graphStateManager(InvRepoManager invRepoManager,
                                      ObjectRepoManager objectRepoManager) {
    return new GraphStateManager(invRepoManager, objectRepoManager);
  }


  @Produces
  @ApplicationScoped
  WebClient webClient(Vertx vertx, InvokerConfig config) {
    WebClientOptions options = new WebClientOptions()
      .setFollowRedirects(false)
      .setMaxPoolSize(config.connectionPoolMaxSize())
      .setHttp2MaxPoolSize(config.h2ConnectionPoolMaxSize())
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setShared(true);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Creating WebClient with options {}", options.toJson());
    }
    return WebClient.create(vertx, options);
  }

  @Produces
  @ApplicationScoped
  HttpOffLoaderConfig invokerConfig(InvokerConfig config) {
    return HttpOffLoaderConfig.builder()
      .appName("oaas/invoker")
      .timout(config.invokeTimeout())
      .build();
  }

  @Produces
  @ApplicationScoped
  OffLoader offLoader(HttpOffLoaderConfig config, WebClient webClient) {
    return new HttpOffLoader(webClient, config);
  }

//  @Produces
//  @ApplicationScoped
//  RouterInvocationReqHandler invocationHandlerService(UnifiedFunctionRouter router, InvocationExecutor invocationExecutor, InvocationQueueProducer sender, InvocationValidator invocationValidator, IdGenerator idGenerator) {
//    return new RouterInvocationReqHandler(router, invocationExecutor, sender, invocationValidator, idGenerator);
//  }

  @Produces
  @Dependent
  ControllerInvocationReqHandler controllerInvocationReqHandler(ClassControllerRegistry classControllerRegistry,
                                                                CtxLoader ctxLoader,
                                                                IdGenerator idGenerator) {
    return new ControllerInvocationReqHandler(classControllerRegistry,
      ctxLoader,
      idGenerator);
  }

  @Produces
  @Dependent
  InvocationRecordHandler invocationRecordHandler(ObjectRepoManager objectRepoManager,
                                                  ClassControllerRegistry classControllerRegistry,
                                                  CtxLoader ctxLoader) {
    return new ControllerInvocationRecordHandler(objectRepoManager, classControllerRegistry, ctxLoader);
  }


  @Produces
  @ApplicationScoped
  HashAwareInvocationHandler hashAwareInvocationHandler(
    LookupManager lookupManager,
    ClassRepository classRepository,
    Vertx vertx,
    ProtoObjectMapper mapper,
    InvocationReqHandler invocationReqHandler,
    IdGenerator idGenerator
  ) {
    return new HashAwareInvocationHandler(
      lookupManager, classRepository, vertx.getDelegate(),
      mapper, invocationReqHandler,
      idGenerator
    );
  }

  @Produces
  @ApplicationScoped
  ContentUrlGenerator contentUrlGenerator(InvokerConfig config) {
    if (config.useSa()) {
      return new SaContentUrlGenerator(config.sa().url());
    } else {
      return new S3ContentUrlGenerator(DatastoreConfRegistry.getDefault()
        .getOrDefault("S3DEFAULT"));
    }
  }


  @Produces
  @Singleton
  IdGenerator idGenerator() {
    return new TsidGenerator();
  }

  @Produces
  @Singleton
  ProtoObjectMapper mapper() {
    var protoObjectMapper = new ProtoObjectMapperImpl();
    protoObjectMapper.setMapper(new MessagePackMapper());
    return protoObjectMapper;
  }
}
