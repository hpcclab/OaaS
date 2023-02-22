package org.hpcclab.oaas.nats;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class NatsProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger( NatsProducer.class );
  @Inject
  NatsConfig config;

  public Connection createConnection(Vertx vertx) throws IOException, InterruptedException {
    var executor = ((VertxInternal) vertx).getWorkerPool().executor();
    return Nats.connect(new Options.Builder()
      .server(config.natsUrls().orElseThrow())
      .executor(executor)
      .build());
  }

  @Produces
  @Dependent
  public ObjectCompletionPublisher completionPublisher(Vertx vertx) throws IOException,
    InterruptedException{
    if (config.natsUrls().isEmpty()) {
      LOGGER.info("No provided NATS URL, the object publisher is disabled");
      return new ObjectCompletionPublisher.Noop();
    }
    return new NatsObjCompPublisher(createConnection(vertx));
  }

  @Produces
  @Dependent
  public ObjectCompletionListener completionListener(Vertx vertx) throws IOException,
    InterruptedException{
    if (config.natsUrls().isEmpty()) {
      LOGGER.info("No provided NATS URL, the object listener is disabled");
      return new ObjectCompletionListener.Noop();
    }

    return new NatsObjCompListener(createConnection(vertx));
  }
}
