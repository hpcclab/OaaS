package org.hpcclab.oaas.invocation.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;

public interface FunctionHandler {
  void validate(FunctionExecContext context);
  Uni<FunctionExecContext> apply(FunctionExecContext context);
}
