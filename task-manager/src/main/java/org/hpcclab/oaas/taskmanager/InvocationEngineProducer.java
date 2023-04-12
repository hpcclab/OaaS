package org.hpcclab.oaas.taskmanager;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.config.HttpInvokerConfig;
import org.hpcclab.oaas.invocation.config.InvocationConfig;
import org.hpcclab.oaas.invocation.handler.AwaitHandler;
import org.hpcclab.oaas.invocation.handler.InvocationHandlerService;
import org.hpcclab.oaas.invocation.validate.InvocationValidator;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InvocationEngineProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvocationEngineProducer.class);

  @Produces
  InvocationExecutor invocationGraphExecutor(
    InvocationQueueSender sender,
    GraphStateManager graphStateManager,
    RepoContextLoader contextLoader,
    SyncInvoker syncInvoker,
    TaskFactory taskFactory,
    CompletedStateUpdater completionHandler) {
    return new InvocationExecutor(sender, graphStateManager, contextLoader, syncInvoker,
      taskFactory, completionHandler);
  }

  @Produces
  InvocationConfig invocationConfig(TaskManagerConfig config) {
    return InvocationConfig.builder()
      .storageAdapterUrl(config.storageAdapterUrl())
      .build();
  }

  @Produces
  WebClient webClient(Vertx vertx, TaskManagerConfig config) {
    WebClientOptions options = new WebClientOptions()
      .setFollowRedirects(false)
      .setMaxPoolSize(config.connectionPoolMaxSize())
      .setHttp2MaxPoolSize(config.h2ConnectionPoolMaxSize())
      .setShared(true);
    LOGGER.info("Creating WebClient with options {}", options.toJson());
    return WebClient.create(vertx, options);
  }

  @Produces
  HttpInvokerConfig invokerConfig() {
    return HttpInvokerConfig.builder()
      .appName("oaas/taskmanager")
      .build();
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

  @Produces
  AwaitHandler awaitHandler(ObjectRepository objectRepo, InvocationExecutor invocationExecutor, InvocationHandlerService invocationHandlerService, ObjectCompletionListener completionListener) {
    return new AwaitHandler(objectRepo, invocationExecutor, invocationHandlerService, completionListener);
  }
}
