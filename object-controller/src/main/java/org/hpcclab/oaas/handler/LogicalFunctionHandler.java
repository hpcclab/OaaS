package org.hpcclab.oaas.handler;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.entity.FunctionExecContext;
import org.hpcclab.oaas.repository.OaasObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class LogicalFunctionHandler {

  @Inject
  OaasObjectRepository objectRepo;


  public Uni<FunctionExecContext> call(FunctionExecContext context) {
    if (context.getFunction().getName().equals("builtin.logical.copy")) {
      var o = context.getMain().copy()
        .setOrigin(context.createOrigin());
      o.setId(null);
      return objectRepo.persist(o)
        .map(context::setOutput);
    } else {
      return null;
    }
  }

  public void validate(FunctionExecContext context) {

  }
}
