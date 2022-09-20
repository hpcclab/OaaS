package org.hpcclab.oaas.taskmanager;

import io.nats.client.Connection;
import io.nats.client.Nats;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class NatsProducer {
  @Inject
  TaskManagerConfig config;

  @Produces
  public Connection nats() throws IOException, InterruptedException {
    return Nats.connect(config.natsUrls());
  }
}
