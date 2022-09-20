package org.hpcclab.oaas.taskmanager.event;

import io.nats.client.Connection;
import org.hpcclab.oaas.repository.event.ObjectCompletionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class NatsObjCompPublisher implements ObjectCompletionPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger( NatsObjCompPublisher.class );
  @Inject
  Connection natsConnection;

  @Override
  public void publish(String objectId) {
    var subject = "objects/" + objectId;
    LOGGER.debug("publish {} to nats", objectId);
    natsConnection.publish(subject, new byte[0]);
  }
}
