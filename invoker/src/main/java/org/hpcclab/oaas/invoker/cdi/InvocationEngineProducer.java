package org.hpcclab.oaas.invoker.cdi;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.invocation.HttpOffLoader;
import org.hpcclab.oaas.invocation.OffLoader;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.controller.*;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.service.ControllerInvocationRecordHandler;
import org.hpcclab.oaas.invoker.service.InvocationRecordHandler;
import org.hpcclab.oaas.invoker.service.S3ContentUrlGenerator;
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
    if (config.useSa()) {
      return new SaContentUrlGenerator(config.sa().url());
    } else {
      return new S3ContentUrlGenerator(config.sa().url(), DatastoreConfRegistry.getDefault()
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
}
