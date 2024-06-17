package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;

/**
 * @author Pawissanutt
 */
public interface LocationAwareInvocationForwarder {
  Uni<InvocationResponse> invoke(InvocationRequest request);
}
