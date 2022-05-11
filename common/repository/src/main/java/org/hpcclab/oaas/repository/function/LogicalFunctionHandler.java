package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class LogicalFunctionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger( LogicalFunctionHandler.class );
  @Inject
  OaasObjectRepository objectRepo;

  public Uni<FunctionExecContext> call(FunctionExecContext context) {
    if (context.getFunction().getName().equals("builtin.logical.copy")) {
      LOGGER.debug("Call function 'copy' {}", context.getMain().getId());
      var o = context.getMain().copy();
      o.setOrigin(context.createOrigin());
      o.setId(objectRepo.generateId());
      return objectRepo.persistAsync(o)
        .map(context::setOutput);
    } else {
      return null;
    }
  }

  public void validate(FunctionExecContext context) {

  }
}
