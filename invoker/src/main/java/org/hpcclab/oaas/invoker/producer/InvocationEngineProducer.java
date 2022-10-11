package org.hpcclab.oaas.invoker.producer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.HttpInvokerConfig;
import org.hpcclab.oaas.invocation.InvocationConfig;
import org.hpcclab.oaas.invocation.RepoContextLoader;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.invocation.function.InvocationGraphExecutor;
import org.hpcclab.oaas.invocation.function.TaskSubmitter;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.repository.GraphStateManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class InvocationEngineProducer {

  @Produces
  InvocationGraphExecutor invocationGraphExecutor(
    TaskSubmitter taskSubmitter,
    GraphStateManager graphStateManager,
    RepoContextLoader contextLoader,
    SyncInvoker syncInvoker) {
    return new InvocationGraphExecutor(taskSubmitter, graphStateManager, contextLoader, syncInvoker);
  }

  @Produces
  InvocationConfig invocationConfig(InvokerConfig invokerConfig) {
    return new InvocationConfig()
      .setStorageAdapterUrl(invokerConfig.storageAdapterUrl());
  }

  @Produces
  WebClient webClient(Vertx vertx) {
    return WebClient.create(vertx);
  }

  @Produces
  HttpInvokerConfig invokerConfig(){
    return HttpInvokerConfig.builder()
      .appName("oaas/invoker")
      .build();
  }
}
