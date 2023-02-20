package org.hpcclab.oaas.invocation.dataflow;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;

public class OneShotDataflowInvoker implements DataflowInvoker{

  @Override
  public Uni<InvApplyingContext> invoke(InvApplyingContext ctx) {
    return null;
  }
}
