package org.hpcclab.msc.stream;

import io.quarkus.runtime.ShutdownEvent;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class KsqlClientProducer {

  @ConfigProperty(name = "ksqlhost", defaultValue = "localhost")
  String host;
  @ConfigProperty(name = "ksqlport", defaultValue = "8088")
  int port;

  @Inject
  Vertx vertx;

  @Produces
  public WebClient webclient() {
    return WebClient.create(vertx, new WebClientOptions().setDefaultHost(host)
      .setDefaultPort(port));
  }


}
