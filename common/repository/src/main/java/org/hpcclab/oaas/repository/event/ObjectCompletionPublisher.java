package org.hpcclab.oaas.repository.event;


public interface ObjectCompletionPublisher {
  void publish(String objectId);

  class Noop implements ObjectCompletionPublisher{
    @Override
    public void publish(String objectId) {
      // NOOP
    }
  }
}
