package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;

/**
 * @author Pawissanutt
 */
public interface InvocationReqHandler {
  Uni<InvocationResponse> syncInvoke(ObjectAccessLanguage oal);
  Uni<InvocationResponse> syncInvoke(InvocationRequest request);
  Uni<InvocationResponse> asyncInvoke(ObjectAccessLanguage oal);
}
