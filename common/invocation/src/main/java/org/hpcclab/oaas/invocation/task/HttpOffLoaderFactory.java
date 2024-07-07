package org.hpcclab.oaas.invocation.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.function.OFunctionConfig;
import org.hpcclab.oaas.proto.FunctionExecutor;
import org.hpcclab.oaas.proto.FunctionExecutorClient;
import org.hpcclab.oaas.proto.MutinyFunctionExecutorGrpc;

import java.net.URI;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class HttpOffLoaderFactory implements OffLoaderFactory {

  final Vertx vertx;
  final HttpOffLoaderConfig httpOffLoaderConfig;
  final ObjectMapper objectMapper;

  public HttpOffLoaderFactory(Vertx vertx,
                              HttpOffLoaderConfig httpOffLoaderConfig,
                              ObjectMapper objectMapper) {
    this.vertx = vertx;
    this.httpOffLoaderConfig = httpOffLoaderConfig;
    this.objectMapper = objectMapper;
  }


  @Override
  public OffLoader create(OFunction function) {
    OFunctionConfig config = function.getConfig();
    if (config==null) config = new OFunctionConfig();
    if (config.getOffloadingMode()==OFunctionConfig.OffloadingMode.GRPC) {
      return new GrpcOffloader(createExecutor(function));
    }
    return createHttp(function.getKey(), config.isHttp2(), config.getOffloadingConfig(),
      createEncoder(function));
  }

  public OffLoader createHttp(String name,
                              boolean http2,
                              Map<String, String> config,
                              TaskEncoder encoder) {
    if (config==null) config = Map.of();
    if (!http2) {
      WebClientOptions options = new WebClientOptions()
        .setMaxPoolSize(httpOffLoaderConfig.getConnectionPoolMaxSize())
        .setHttp2MaxPoolSize(httpOffLoaderConfig.getH2ConnectionPoolMaxSize())
        .setConnectTimeout(httpOffLoaderConfig.getConnectTimeout())
        .setKeepAliveTimeout(httpOffLoaderConfig.getKeepaliveTimeout())
        .setShared(true)
        .setName(name)
        .setKeepAlive(config.getOrDefault("keepAlive", "true")
          .equalsIgnoreCase("true"));
      return new HttpOffLoader(
        WebClient.create(vertx, options),
        encoder,
        httpOffLoaderConfig
      );
    } else {
      WebClientOptions options = new WebClientOptions()
        .setFollowRedirects(false)
        .setMaxPoolSize(httpOffLoaderConfig.getConnectionPoolMaxSize())
        .setHttp2MaxPoolSize(httpOffLoaderConfig.getH2ConnectionPoolMaxSize())
        .setConnectTimeout(httpOffLoaderConfig.getConnectTimeout())
        .setKeepAliveTimeout(httpOffLoaderConfig.getKeepaliveTimeout())
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setHttp2ClearTextUpgrade(false)
        .setShared(true)
        .setName("Http2OffLoader");
      return new HttpOffLoader(
        WebClient.create(vertx, options),
        encoder,
        httpOffLoaderConfig
      );

    }
  }

  TaskEncoder createEncoder(OFunction function) {
    if (function.getConfig().getOffloadingMode() == OFunctionConfig.OffloadingMode.PROTOBUF) {
      return new ProtobufTaskEncoder();
    }
    return new JsonTaskEncoder(objectMapper);
  }

  FunctionExecutor createExecutor(OFunction function) {
    URI uri = URI.create(function.getStatus().getInvocationUrl());
    VertxChannelBuilder vertxChannelBuilder = VertxChannelBuilder.forAddress(
        vertx.getDelegate(), uri.getHost(), uri.getPort())
      .disableRetry()
      .usePlaintext();
    return new FunctionExecutorClient(function.getKey(),
      vertxChannelBuilder.build(), this::configure);
  }

  MutinyFunctionExecutorGrpc.MutinyFunctionExecutorStub configure(
    String key,
    MutinyFunctionExecutorGrpc.MutinyFunctionExecutorStub stub) {
    return stub;
  }
}
