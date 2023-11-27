package org.hpcclab.oaas.invoker.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.invocation.validate.InvocationValidator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.ispn.lookup.LookupManager;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.invoker.service.S3ContentUrlGenerator;
import org.hpcclab.oaas.repository.*;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;
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
    return new InvocationExecutor(sender, graphStateManager, contextLoader, offLoader, taskFactory, completionHandler);
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

  @Produces
  @ApplicationScoped
  InvocationReqHandler invocationHandlerService(UnifiedFunctionRouter router, InvocationExecutor invocationExecutor, InvocationQueueProducer sender, InvocationValidator invocationValidator, IdGenerator idGenerator) {
    return new InvocationReqHandler(router, invocationExecutor, sender, invocationValidator, idGenerator);
  }

  @Produces
  @ApplicationScoped
  HashAwareInvocationHandler hashAwareInvocationHandler(
    LookupManager lookupManager, ClassRepository classRepository, Vertx vertx, ObjectMapper objectMapper, InvocationReqHandler invocationReqHandler
  ) {
    return new HashAwareInvocationHandler(
      lookupManager, classRepository, vertx.getDelegate(),
      objectMapper, invocationReqHandler
    );
  }

  @Produces
  @ApplicationScoped
  ContentUrlGenerator contentUrlGenerator(InvokerConfig config) {
    if (config.useSa()) {
      return new SaContentUrlGenerator(config.storageAdapterUrl());
    } else {
      return new S3ContentUrlGenerator(config);
    }
  }


  @Produces
  @Singleton
  IdGenerator idGenerator() {
    return new TsidGenerator();
  }
}
