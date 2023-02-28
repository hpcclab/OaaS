package org.hpcclab.oaas.taskmanager;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.config.HttpInvokerConfig;
import org.hpcclab.oaas.invocation.config.InvocationConfig;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class InvocationEngineProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger( InvocationEngineProducer.class );

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
  HttpInvokerConfig invokerConfig(){
    return HttpInvokerConfig.builder()
      .appName("oaas/taskmanager")
      .build();
  }
}
