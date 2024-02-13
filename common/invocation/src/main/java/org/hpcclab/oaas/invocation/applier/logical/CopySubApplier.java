package org.hpcclab.oaas.invocation.applier.logical;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.applier.LogicalSubApplier;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.repository.id.IdGenerator;

public class CopySubApplier implements LogicalSubApplier {

  IdGenerator idGenerator;

  @Override
  public void validate(InvocationContext context) {

  }

  @Override
  public Uni<InvocationContext> apply(InvocationContext context) {
    var o = context.getMain().copy();
    o.setId(idGenerator.generate(context));
    context.setOutput(o);
    return Uni.createFrom().item(context);
  }

  @Override
  public String functionKey() {
    return "builtin.logical.copy";
  }
}
