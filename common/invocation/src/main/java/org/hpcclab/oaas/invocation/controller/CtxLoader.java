package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;

/**
 * @author Pawissanutt
 */
public interface CtxLoader {

  Uni<InvocationCtx> load(InvocationRequest request);
  Uni<InvocationCtx> load(ProtoInvocationRequest request);
}
