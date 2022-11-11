package org.hpcclab.oaas.repository.event;

import io.smallrye.mutiny.Uni;

public interface ObjectCompletionListener {
  void cleanup();
  Uni<String> wait(String id, Integer timeout);
  default boolean healthcheck() {return true;}

  class Noop implements ObjectCompletionListener{

    @Override
    public void cleanup() {
      // NOOP
    }

    @Override
    public Uni<String> wait(String id, Integer timeout) {
      return Uni.createFrom().item(id);
    }
  }
}
