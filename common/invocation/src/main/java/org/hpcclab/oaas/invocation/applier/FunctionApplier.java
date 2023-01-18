package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;

public interface FunctionApplier {
  void validate(FunctionExecContext context);
  Uni<FunctionExecContext> apply(FunctionExecContext context);
}
