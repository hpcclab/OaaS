package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.repository.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class LogicalFunctionHandler implements FunctionHandler{

  private static final Logger LOGGER = LoggerFactory.getLogger( LogicalFunctionHandler.class );
  IdGenerator idGenerator;

  @Inject
  public LogicalFunctionHandler(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  public Uni<FunctionExecContext> call(FunctionExecContext context) {
    if (context.getFunction().getName().equals("builtin.logical.copy")) {
      LOGGER.debug("Call function 'copy' {}", context.getMain().getId());
      var o = context.getMain().copy();
      o.setOrigin(context.createOrigin());
      o.setId(idGenerator.generate(context));
      context.setOutput(o);
      return Uni.createFrom().item(context);
    } else {
      return null;
    }
  }

  public void validate(FunctionExecContext context) {

  }
}
