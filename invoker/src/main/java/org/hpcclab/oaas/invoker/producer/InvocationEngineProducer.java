package org.hpcclab.oaas.invoker.producer;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.config.HttpInvokerConfig;
import org.hpcclab.oaas.invocation.config.InvocationConfig;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.TaskSubmitter;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
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
    CompletionValidator completionValidator) {
    return new InvocationExecutor(sender, graphStateManager, contextLoader, syncInvoker, taskFactory, completionValidator);
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
  HttpInvokerConfig invokerConfig(InvokerConfig config){
    return HttpInvokerConfig.builder()
      .appName("oaas/invoker")
      .timout(config.invokeTimeout())
      .build();
  }
}
