package org.hpcclab.oaas.invoker;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.HttpInvokerConfig;
import org.hpcclab.oaas.invocation.InvocationConfig;
import org.hpcclab.oaas.invocation.RepoContextLoader;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.invocation.function.InvocationGraphExecutor;
import org.hpcclab.oaas.invocation.function.TaskSubmitter;
import org.hpcclab.oaas.repository.GraphStateManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class InvocationEngineProducer {

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
