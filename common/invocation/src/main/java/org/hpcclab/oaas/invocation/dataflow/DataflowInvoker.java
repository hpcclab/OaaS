package org.hpcclab.oaas.invocation.dataflow;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationContext;

public interface DataflowInvoker {
  Uni<InvocationContext> invoke(InvocationContext ctx);
}
