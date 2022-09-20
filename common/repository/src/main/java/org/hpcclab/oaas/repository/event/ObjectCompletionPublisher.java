package org.hpcclab.oaas.repository.event;


public interface ObjectCompletionPublisher {
  void publish(String objectId);
}
