package org.hpcclab.oaas.repository.event;

import io.smallrye.mutiny.Uni;

public interface ObjectCompletionListener {
  void cleanup();
  Uni<String> wait(String id, Integer timeout);
  default boolean healthcheck() {return true;}
}
