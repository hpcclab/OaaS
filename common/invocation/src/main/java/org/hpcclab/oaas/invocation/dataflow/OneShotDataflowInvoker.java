package org.hpcclab.oaas.invocation.dataflow;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;

public class OneShotDataflowInvoker implements DataflowInvoker{

  @Override
  public Uni<FunctionExecContext> invoke(FunctionExecContext ctx) {
    return null;
  }
}
