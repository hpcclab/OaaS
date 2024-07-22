package org.hpcclab.oaas.invocation.state;

import io.smallrye.mutiny.Uni;

/**
 * @author Pawissanutt
 */
public interface StateManager {
  Uni<Void> applySimple(SimpleStateOperation operation);
  Uni<Void> applyDelete(DeleteStateOperation operation);
}
