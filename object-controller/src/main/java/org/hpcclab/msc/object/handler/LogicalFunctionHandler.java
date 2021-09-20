package org.hpcclab.msc.object.handler;

import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;
import org.hpcclab.msc.object.model.FunctionExecContext;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LogicalFunctionHandler {
  public MscObject call(FunctionExecContext context) {
    if (context.getFunction().getName().equals("builtin.logical.copy")) {
      return context.getTarget().copy().setId(null)
        .setOrigin(new MscObjectOrigin(context));
    } else {
      return null;
    }
  }

  public void validate(FunctionExecContext context) {

  }
}
