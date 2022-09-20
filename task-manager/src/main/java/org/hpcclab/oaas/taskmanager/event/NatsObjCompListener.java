package org.hpcclab.oaas.taskmanager.event;

import io.nats.client.Connection;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class NatsObjCompListener implements ObjectCompletionListener {
  private static final Logger LOGGER = LoggerFactory.getLogger( NatsObjCompListener.class );
  @Inject
  Connection nc;
  @Override
  public void cleanup() {
    try {
      nc.close();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Uni<String> wait(String id, Integer timeout) {
    var subject = "objects/" + id;
    return Uni.createFrom().emitter(emitter -> {
      LOGGER.debug("start subscribe to {}", id);
      var dispatcher = nc.createDispatcher((msg) -> {
        LOGGER.debug("receive event from {}", id);
        emitter.complete(id);
      });
      emitter.onTermination(() -> dispatcher.unsubscribe(subject));
      dispatcher.subscribe(subject);
    });
  }

  @Override
  public boolean healthcheck() {
    return nc.getStatus() == Connection.Status.CONNECTED;
  }
}
