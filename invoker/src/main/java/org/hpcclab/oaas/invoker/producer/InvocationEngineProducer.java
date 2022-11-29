package org.hpcclab.oaas.invoker.producer;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.*;
import org.hpcclab.oaas.invocation.function.InvocationGraphExecutor;
import org.hpcclab.oaas.invocation.function.TaskSubmitter;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.repository.GraphStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class InvocationEngineProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger( InvocationEngineProducer.class );

  @Produces
  InvocationGraphExecutor invocationGraphExecutor(
    TaskSubmitter taskSubmitter,
    GraphStateManager graphStateManager,
    ContextLoader contextLoader,
    SyncInvoker syncInvoker) {
    return new InvocationGraphExecutor(taskSubmitter, graphStateManager, contextLoader, syncInvoker);
  }

  @Produces
  InvocationConfig invocationConfig(InvokerConfig invokerConfig) {
    return new InvocationConfig()
      .setStorageAdapterUrl(invokerConfig.storageAdapterUrl());
  }

  @Produces
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
  HttpInvokerConfig invokerConfig(){
    return HttpInvokerConfig.builder()
      .appName("oaas/invoker")
      .build();
  }
}
