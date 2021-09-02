package org.hpcclab.msc.object.service;

import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionExecContext;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class LogicalFunctionHandler {
  public MscObject call(FunctionExecContext context) {
    if (context.getFunction().getName().equals("buildin.logical.copy")) {
      return context.getTarget().copy().setId(null);
    } else {
      return null;
    }
  }
}
