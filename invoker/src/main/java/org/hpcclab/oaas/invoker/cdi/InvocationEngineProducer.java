package org.hpcclab.oaas.invoker.cdi;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.controller.*;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.HttpOffLoader;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.service.ControllerInvocationRecordHandler;
import org.hpcclab.oaas.invoker.service.InvocationRecordHandler;
import org.hpcclab.oaas.invoker.service.UnifyContentUrlGenerator;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.mapper.ProtoObjectMapperImpl;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InvocationEngineProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvocationEngineProducer.class);

  @Produces
  @ApplicationScoped
  WebClient webClient(Vertx vertx, InvokerConfig config) {
    WebClientOptions options = new WebClientOptions()
      .setFollowRedirects(false)
      .setMaxPoolSize(config.connectionPoolMaxSize())
      .setHttp2MaxPoolSize(config.h2ConnectionPoolMaxSize())
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setName("httpOffLoader")
      .setShared(true);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Creating WebClient with options {}", options.toJson());
    }
    return WebClient.create(vertx, options);
  }

  @Produces
  @ApplicationScoped
  HttpOffLoaderConfig httpOffLoaderConfig(InvokerConfig config) {
    return HttpOffLoaderConfig.builder()
      .appName("oaas/invoker")
      .timout(config.invokeTimeout())
      .enabledCeHeader(config.enableCeHeaderOffload())
      .build();
  }

  @Produces
  @ApplicationScoped
  OffLoader offLoader(HttpOffLoaderConfig config, WebClient webClient) {
    return new HttpOffLoader(webClient, config);
  }


  @Produces
  @ApplicationScoped
  ControllerInvocationReqHandler controllerInvocationReqHandler(ClassControllerRegistry classControllerRegistry,
                                                                CtxLoader ctxLoader,
                                                                IdGenerator idGenerator) {
    return new ControllerInvocationReqHandler(classControllerRegistry,
      ctxLoader,
      idGenerator);
  }

  @Produces
  @ApplicationScoped
  InvocationRecordHandler invocationRecordHandler(ObjectRepoManager objectRepoManager,
                                                  ClassControllerRegistry classControllerRegistry,
                                                  CtxLoader ctxLoader) {
    return new ControllerInvocationRecordHandler(objectRepoManager, classControllerRegistry, ctxLoader);
  }


  @Produces
  @ApplicationScoped
  ContentUrlGenerator contentUrlGenerator(InvokerConfig config) {
    if (config.useSaOnly()) {
      return new SaContentUrlGenerator(config.sa().url());
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

  @Produces
  @Singleton
  ProtoObjectMapper mapper() {
    var protoObjectMapper = new ProtoObjectMapperImpl();
    protoObjectMapper.setMapper(new MessagePackMapper());
    return protoObjectMapper;
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
      .setShared(true)
      .setName("grpc")
    );
  }
}
