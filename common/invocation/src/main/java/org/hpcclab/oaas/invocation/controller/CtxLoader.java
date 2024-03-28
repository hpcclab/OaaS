package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

/**
 * @author Pawissanutt
 */
public interface CtxLoader {

  Uni<InvocationCtx> load(InvocationRequest request);
}
