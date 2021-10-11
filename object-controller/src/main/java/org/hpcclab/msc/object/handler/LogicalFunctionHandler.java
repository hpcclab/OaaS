package org.hpcclab.msc.object.handler;

import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.object.OaasObjectOrigin;
import org.hpcclab.msc.object.model.FunctionExecContext;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LogicalFunctionHandler {
  public OaasObject call(FunctionExecContext context) {
    if (context.getFunction().getName().equals("builtin.logical.copy")) {
      return context.getMain().copy().setId(null)
        .setOrigin(new OaasObjectOrigin(context));
    } else {
      return null;
    }
  }

  public void validate(FunctionExecContext context) {

  }
}
