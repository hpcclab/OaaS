package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;

/**
 * @author Pawissanutt
 */
public class InvocationChainProcessor {

  Uni<InvocationCtx> handle(InvocationCtx ctx) {
    //TODO
    return Uni.createFrom().item(ctx);
  }
}
