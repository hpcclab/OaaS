package org.hpcclab.oaas.invoker.service;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.invocation.task.HttpOffLoader;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.function.OFunctionConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class HttpOffLoaderFactory implements OffLoaderFactory {

  final Vertx vertx;
  final InvokerConfig invokerConfig;

  public HttpOffLoaderFactory(Vertx vertx, InvokerConfig invokerConfig) {
    this.vertx = vertx;
    this.invokerConfig = invokerConfig;
  }


  @Override
  public OffLoader create(OFunction function) {
    OFunctionConfig config = function.getConfig();
    if (config == null) config = new OFunctionConfig();
    var type = config.isHttp2() ? "http2" : "http1.1";
    return create(type, config.getOffloadingConfig());
  }

  @Override
  public OffLoader create(String type, Map<String, String> config) {
    if (type.equalsIgnoreCase("http1.1")) {
      WebClientOptions options = new WebClientOptions()
        .setMaxPoolSize(invokerConfig.connectionPoolMaxSize())
        .setHttp2MaxPoolSize(invokerConfig.h2ConnectionPoolMaxSize())
        .setShared(true)
        .setName("HttpOffLoader")
        ;
      return new HttpOffLoader(WebClient.create(
        vertx,
        options),
        httpOffLoaderConfig()
      );
    } else if (type.equalsIgnoreCase("http2")) {
      WebClientOptions options = new WebClientOptions()
        .setFollowRedirects(false)
        .setMaxPoolSize(invokerConfig.connectionPoolMaxSize())
        .setHttp2MaxPoolSize(invokerConfig.h2ConnectionPoolMaxSize())
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setHttp2ClearTextUpgrade(false)
        .setShared(true)
        .setName("Http2OffLoader")
        ;
      return new HttpOffLoader(WebClient.create(
        vertx,
        options),
        httpOffLoaderConfig()
      );

    }
    throw new IllegalArgumentException();
  }


  HttpOffLoaderConfig httpOffLoaderConfig() {
    return HttpOffLoaderConfig.builder()
      .appName("oaas/invoker")
      .timout(invokerConfig.invokeTimeout())
      .enabledCeHeader(invokerConfig.enableCeHeaderOffload())
      .build();
  }
}
