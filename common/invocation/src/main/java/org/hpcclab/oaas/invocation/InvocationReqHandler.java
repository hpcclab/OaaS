package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;

/**
 * @author Pawissanutt
 */
public interface InvocationReqHandler {
  Uni<InvocationResponse> invoke(ObjectAccessLanguage oal);
  Uni<InvocationResponse> invoke(InvocationRequest request);
  Uni<InvocationResponse> enqueue(ObjectAccessLanguage oal);
}
