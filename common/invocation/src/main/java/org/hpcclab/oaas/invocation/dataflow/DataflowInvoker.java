package org.hpcclab.oaas.invocation.dataflow;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;

public interface DataflowInvoker {
  Uni<InvApplyingContext> invoke(InvApplyingContext ctx);
}
