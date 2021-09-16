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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@ApplicationScoped
public class KsqlClientProducer {

  @ConfigProperty(name = "oaas.sc.ksqlUrl", defaultValue = "localhost")
  String ksqlurl;

  @Inject
  Vertx vertx;

  @Produces
  public WebClient webclient() throws MalformedURLException {
    URL uri = new URL(ksqlurl);
    return WebClient.create(vertx, new WebClientOptions().setDefaultHost(uri.getHost())
      .setDefaultPort(uri.getPort() > 0 ? uri.getPort() : uri.getDefaultPort()));
  }


}
