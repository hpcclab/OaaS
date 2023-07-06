package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationContext;

public interface FunctionApplier {
  void validate(InvocationContext context);
  Uni<InvocationContext> apply(InvocationContext context);
}
