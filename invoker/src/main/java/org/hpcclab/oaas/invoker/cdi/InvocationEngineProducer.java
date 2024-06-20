package org.hpcclab.oaas.invoker.cdi;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.controller.*;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.DefaultContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.HttpOffLoaderFactory;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.metrics.MicrometerMetricFactory;
import org.hpcclab.oaas.invoker.metrics.RequestCounterMap;
import org.hpcclab.oaas.invoker.service.CcInvocationRecordHandler;
import org.hpcclab.oaas.invoker.service.InvocationRecordHandler;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.hpcclab.oaas.storage.UnifyContentUrlGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InvocationEngineProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvocationEngineProducer.class);


  @Produces
  @ApplicationScoped
  InvocationReqHandler invocationReqHandler(ClassControllerRegistry classControllerRegistry,
                                            CtxLoader ctxLoader,
                                            IdGenerator idGenerator,
                                            InvokerConfig invokerConfig) {
    return new CcInvocationReqHandler(classControllerRegistry,
      ctxLoader,
      idGenerator,
      invokerConfig.maxInflight()
    );
  }

  @Produces
  @ApplicationScoped
  InvocationRecordHandler invocationRecordHandler(ObjectRepoManager objectRepoManager,
                                                  ClassControllerRegistry classControllerRegistry,
                                                  CtxLoader ctxLoader) {
    return new CcInvocationRecordHandler(objectRepoManager,
      classControllerRegistry,
      ctxLoader);
  }


  @Produces
  @ApplicationScoped
  ContentUrlGenerator contentUrlGenerator(InvokerConfig config) {
    if (config.useSaOnly()) {
      return new DefaultContentUrlGenerator(config.sa().url());
    } else {
      return new UnifyContentUrlGenerator(config.sa().url(),
        DatastoreConfRegistry.getDefault()
          .getOrDefault("S3DEFAULT"));
    }
  }


  @Produces
  @Singleton
  IdGenerator idGenerator() {
    return new TsidGenerator();
  }


  @ApplicationScoped
  @Produces
  RepoStateManager repoStateManager(ObjectRepoManager repoManager) {
    return new RepoStateManager(repoManager);
  }

  @ApplicationScoped
  @Produces
  RepoCtxLoader repoCtxLoader(ObjectRepoManager objManager,
                              ClassControllerRegistry registry) {
    return new RepoCtxLoader(objManager, registry);
  }

  @Produces
  @ApplicationScoped
  GrpcClient grpcClient(Vertx vertx, InvokerConfig config) {
    return GrpcClient.client(vertx.getDelegate(), new HttpClientOptions()
      .setMaxPoolSize(config.connectionPoolMaxSize())
      .setHttp2MaxPoolSize(config.h2ConnectionPoolMaxSize())
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setHttp2ClearTextUpgrade(false)
      .setConnectTimeout(config.connectTimeout())
      .setIdleTimeout(30)
    );
  }

  @ApplicationScoped
  @Produces
  RequestCounterMap requestCounterMap(InvokerConfig config,
                                      MicrometerMetricFactory factory,
                                      ClassControllerRegistry registry) {
    if (config.enableInvReqMetric()) {
      return new RequestCounterMap(factory, registry);
    } else {
      return new RequestCounterMap.NoOp();
    }
  }


  @ApplicationScoped
  @Produces
  HttpOffLoaderFactory factory(Vertx vertx, InvokerConfig invokerConfig) {
    HttpOffLoaderConfig config = HttpOffLoaderConfig.builder()
      .h2ConnectionPoolMaxSize(invokerConfig.h2ConnectionPoolMaxSize())
      .appName("oparaca/invoker")
      .timout(invokerConfig.invokeTimeout())
      .connectTimeout(invokerConfig.connectTimeout())
      .enabledCeHeader(invokerConfig.enableCeHeaderOffload())
      .h2ConnectionPoolMaxSize(invokerConfig.h2ConnectionPoolMaxSize())
      .build();
    return new HttpOffLoaderFactory(vertx, config);
  }

}
