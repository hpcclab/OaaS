package org.hpcclab.oaas.handler;

import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.FunctionExecContext;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LogicalFunctionHandler {
  public OaasObject call(FunctionExecContext context) {
    if (context.getFunction().getName().equals("builtin.logical.copy")) {
      var o = context.getMain().copy()
        .setOrigin(new OaasObjectOrigin(context));
      o.setId(null);
      return o;
    } else {
      return null;
    }
  }

  public void validate(FunctionExecContext context) {

  }
}
