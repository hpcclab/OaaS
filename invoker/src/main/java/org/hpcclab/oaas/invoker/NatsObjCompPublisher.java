package org.hpcclab.oaas.invoker;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;

@Dependent
public class NatsObjCompPublisher implements ObjectCompletionPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(NatsObjCompPublisher.class);

  @Inject
  InvokerConfig config;
  @Inject
  Vertx vertx;

  Connection natsConnection;

  @PostConstruct
  void setup() throws IOException, InterruptedException {
    var executor = ((VertxInternal) vertx).getWorkerPool().executor();
    natsConnection = Nats.connect(new Options.Builder()
      .server(config.natsUrls())
      .executor(executor)
      .build());
  }
  @Override
  public void publish(String objectId) {
    var subject = "objects/" + objectId;
    LOGGER.debug("publish {} to nats", objectId);
    natsConnection.publish(subject, new byte[0]);
  }
}
