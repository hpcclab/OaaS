package org.hpcclab.oaas.crm.cdi;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class VertxProducer {
  @Produces
  @ApplicationScoped
  WebClient webClient(Vertx vertx) {
    WebClientOptions options = new WebClientOptions()
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setShared(true);
    return WebClient.create(vertx, options);
  }
}
