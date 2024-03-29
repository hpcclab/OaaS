package org.hpcclab.oaas.invoker.producer;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.invocation.validate.InvocationValidator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.service.S3ContentUrlGenerator;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.hpcclab.oaas.repository.InvNodeRepository;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.id.IdGenerator;
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
  GraphStateManager graphStateManager(InvNodeRepository invNodeRepo,
                                      ObjectRepository objRepo) {
    return new GraphStateManager(invNodeRepo, objRepo);
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
  InvocationReqHandler invocationHandlerService(UnifiedFunctionRouter router, InvocationExecutor invocationExecutor, InvocationQueueProducer sender, InvocationValidator invocationValidator, IdGenerator idGenerator) {
    return new InvocationReqHandler(router, invocationExecutor, sender, invocationValidator, idGenerator);
  }

  @Produces
  ContentUrlGenerator contentUrlGenerator(InvokerConfig config) {
    if (config.useSa()) {
      return new SaContentUrlGenerator(config.storageAdapterUrl());
    } else {
      return new S3ContentUrlGenerator(config);
    }
  }
}
