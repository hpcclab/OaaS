package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;

/**
 * @author Pawissanutt
 */
public interface StateManager {
  Uni<Void> applySimple(SimpleStateOperation operation);
}
