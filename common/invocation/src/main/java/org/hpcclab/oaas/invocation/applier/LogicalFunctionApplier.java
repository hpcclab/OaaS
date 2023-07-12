package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LogicalFunctionApplier implements FunctionApplier {

  private static final Logger LOGGER = LoggerFactory.getLogger( LogicalFunctionApplier.class );
  IdGenerator idGenerator;

  @Inject
  public LogicalFunctionApplier(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  public Uni<InvocationContext> apply(InvocationContext context) {
    if (context.getFunction().getKey().equals("builtin.logical.copy")) {
      LOGGER.debug("Call func 'copy' {}", context.getMain().getId());
      var o = context.getMain().copy();
//      o.setOrigin(context.createOrigin());
      o.setId(idGenerator.generate(context));
      context.setOutput(o);
      return Uni.createFrom().item(context);
    } else {
      return null;
    }
  }

  public void validate(InvocationContext context) {

  }
}
