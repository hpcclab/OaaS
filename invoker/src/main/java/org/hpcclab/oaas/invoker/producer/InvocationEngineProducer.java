package org.hpcclab.oaas.invoker.producer;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.config.HttpOffloaderConfig;
import org.hpcclab.oaas.invocation.config.InvocationConfig;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.handler.InvocationHandlerService;
import org.hpcclab.oaas.invocation.task.TaskFactory;
import org.hpcclab.oaas.invocation.validate.InvocationValidator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class InvocationEngineProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger( InvocationEngineProducer.class );

  @Produces
  InvocationExecutor invocationGraphExecutor(
    InvocationQueueSender sender,
    GraphStateManager graphStateManager,
    RepoContextLoader contextLoader,
    OffLoader offLoader,
    TaskFactory taskFactory,
    CompletedStateUpdater completionHandler) {
    return new InvocationExecutor(sender, graphStateManager, contextLoader, offLoader, taskFactory, completionHandler);
  }

  @Produces
  @ApplicationScoped
  InvocationConfig invocationConfig(InvokerConfig invokerConfig) {
    return InvocationConfig.builder()
      .storageAdapterUrl(invokerConfig.storageAdapterUrl())
      .build();
  }

  @Produces
  @ApplicationScoped
  WebClient webClient(Vertx vertx, InvokerConfig config) {
    WebClientOptions options = new WebClientOptions()
      .setFollowRedirects(false)
      .setMaxPoolSize(config.connectionPoolMaxSize())
      .setHttp2MaxPoolSize(config.h2ConnectionPoolMaxSize())
      .setShared(true)
      ;
    LOGGER.info("Creating WebClient with options {}", options.toJson());
    return WebClient.create(vertx, options);
  }

  @Produces
  @ApplicationScoped
  HttpOffloaderConfig invokerConfig(InvokerConfig config){
    return HttpOffloaderConfig.builder()
      .appName("oaas/invoker")
      .timout(config.invokeTimeout())
      .build();
  }

  @Produces
  @ApplicationScoped
  OffLoader offLoader(HttpOffloaderConfig config, WebClient webClient) {
    return new HttpOffLoader(webClient, config);
  }

  @Produces
  ObjectCompletionListener completionListener() {
    return new ObjectCompletionListener.Noop();
  }

  @Produces
  ObjectCompletionPublisher completionPublisher() {
    return new ObjectCompletionPublisher.Noop();
  }


  @Produces
  InvocationHandlerService invocationHandlerService(UnifiedFunctionRouter router, InvocationExecutor invocationExecutor, InvocationQueueSender sender, InvocationValidator invocationValidator, IdGenerator idGenerator) {
    return new InvocationHandlerService(router, invocationExecutor, sender, invocationValidator, idGenerator);
  }

}
