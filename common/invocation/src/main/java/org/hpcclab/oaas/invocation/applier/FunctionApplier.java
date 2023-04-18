package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;

public interface FunctionApplier {
  void validate(InvApplyingContext context);
  Uni<InvApplyingContext> apply(InvApplyingContext context);
}
