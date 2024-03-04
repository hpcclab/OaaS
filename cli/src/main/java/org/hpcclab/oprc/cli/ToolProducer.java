package org.hpcclab.oprc.cli;

import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Singleton
public class ToolProducer {

//  @Produces
//  @Singleton
//  Vertx vertx(){
//    return Vertx.vertx();
//  }

  @Produces
  @ApplicationScoped
  WebClient webClient(Vertx vertx,
                      ConfigFileManager fileManager) throws IOException {
    String proxyString = fileManager.current().getProxy();

    WebClientOptions webClientOptions = new WebClientOptions()
      .setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1))
      ;
    if (proxyString!=null) {
      var proxy = URI.create(proxyString);
      if (!proxy.isAbsolute()) {
        throw new IllegalStateException("Proxy URL is not valid (" + proxyString + ")");
      }

      var type = switch (proxy.getScheme()) {
        case "socks5" -> ProxyType.SOCKS5;
        case "socks4" -> ProxyType.SOCKS4;
        case "http" -> ProxyType.HTTP;
        default -> throw new IllegalArgumentException("Unsupported proxy protocol of " + proxy.getScheme());
      };
      var auth = proxy.getUserInfo();
      var proxyOptions = new ProxyOptions().setType(type)
        .setHost(proxy.getHost());
      if (proxy.getPort() > 0) {
        proxyOptions.setPort(proxy.getPort());
      }
      if (!auth.isEmpty()) {
        var splitAuth = auth.split(":");
        proxyOptions.setUsername(splitAuth[0]);
        if (splitAuth.length >= 2) {
          proxyOptions.setPassword(splitAuth[1]);
        }
      }
      webClientOptions.setProxyOptions(proxyOptions);
    }
    return WebClient.create(vertx,
      webClientOptions
    );
  }

  @Produces
  @ApplicationScoped
  GrpcClient grpcClient(Vertx vertx) throws IOException {
    return GrpcClient.client(vertx.getDelegate());
  }

//  @Produces
//  @Singleton
//  Vertx vertx() {
//    return Vertx.vertx();
//  }
}
