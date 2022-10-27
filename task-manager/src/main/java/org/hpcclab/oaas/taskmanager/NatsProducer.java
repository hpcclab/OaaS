package org.hpcclab.oaas.taskmanager;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class NatsProducer {
  @Inject
  TaskManagerConfig config;

  @Produces
  public Connection nats(Vertx vertx) throws IOException, InterruptedException {
    var executor = ((VertxInternal) vertx).getWorkerPool().executor();
    return Nats.connect(new Options.Builder()
      .server(config.natsUrls())
      .executor(executor)
      .build());
  }
}
