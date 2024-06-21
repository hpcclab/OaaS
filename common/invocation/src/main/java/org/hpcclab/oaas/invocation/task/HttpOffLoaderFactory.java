package org.hpcclab.oaas.invocation.task;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.function.OFunctionConfig;

import java.util.Map;

/**
 * @author Pawissanutt
 */
public class HttpOffLoaderFactory implements OffLoaderFactory {

  final Vertx vertx;
  final HttpOffLoaderConfig httpOffLoaderConfig;

  public HttpOffLoaderFactory(Vertx vertx, HttpOffLoaderConfig httpOffLoaderConfig) {
    this.vertx = vertx;
    this.httpOffLoaderConfig = httpOffLoaderConfig;
  }


  @Override
  public OffLoader create(OFunction function) {
    OFunctionConfig config = function.getConfig();
    if (config==null) config = new OFunctionConfig();
    var type = config.isHttp2() ? "http2":"http1.1";
    return create(function.getKey(), type, config.getOffloadingConfig());
  }

  public OffLoader create(String name, String type, Map<String, String> config) {
    if (config==null) config = Map.of();
    if (type.equalsIgnoreCase("http1.1")) {
      WebClientOptions options = new WebClientOptions()
        .setMaxPoolSize(httpOffLoaderConfig.getConnectionPoolMaxSize())
        .setHttp2MaxPoolSize(httpOffLoaderConfig.getH2ConnectionPoolMaxSize())
        .setConnectTimeout(httpOffLoaderConfig.getConnectTimeout())
        .setShared(true)
        .setName(name)
        .setKeepAlive(config.getOrDefault("keepAlive", "true")
          .equalsIgnoreCase("true"));
      return new HttpOffLoader(WebClient.create(
        vertx,
        options),
        httpOffLoaderConfig
      );
    } else if (type.equalsIgnoreCase("http2")) {
      WebClientOptions options = new WebClientOptions()
        .setFollowRedirects(false)
        .setMaxPoolSize(httpOffLoaderConfig.getConnectionPoolMaxSize())
        .setHttp2MaxPoolSize(httpOffLoaderConfig.getH2ConnectionPoolMaxSize())
        .setConnectTimeout(httpOffLoaderConfig.getConnectTimeout())
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setHttp2ClearTextUpgrade(false)
        .setShared(true)
        .setName("Http2OffLoader");
      return new HttpOffLoader(WebClient.create(
        vertx,
        options),
        httpOffLoaderConfig
      );

    }
    throw new IllegalArgumentException();
  }

}
