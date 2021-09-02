package org.hpcclab.msc.object.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceThrowable;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.NoStackException;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskFunctionHandler {

  public void validate(FunctionExecContext context) {
    if (!context.getTarget().getFunctions().contains(context.getFunction().getName()))
      throw new NoStackException("Can not call this function")
        .setCode(400);
  }

  public Uni<MscObject> call(FunctionExecContext context) {
    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
  }

  public Uni<MscObject> subCall(FunctionExecContext context) {
    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
  }
}
