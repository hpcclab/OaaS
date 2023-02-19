package org.hpcclab.oaas.invocation.dataflow;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;

public interface DataflowInvoker {
  Uni<FunctionExecContext> invoke(FunctionExecContext ctx);
}
