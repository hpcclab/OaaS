package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.invocation.OObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TaskFunctionApplier implements FunctionApplier {
  private static final Logger logger = LoggerFactory.getLogger(TaskFunctionApplier.class);

  OObjectFactory objectFactory;


  public TaskFunctionApplier(OObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public void validate(InvocationContext context) {
    if (context.getMainCls()==null)
      throw FunctionValidationException.format(
        "cls('%s') is not exist or cannot be loaded",
        context.getRequest().cls()
      );
  }

  public Uni<InvocationContext> apply(InvocationContext ctx) {
    ctx.setImmutable(ctx.getFb().isForceImmutable() || !ctx.getFunction().getType().isMutable());
    var req = ctx.getRequest();
    var outId = req!= null? req.outId() : null;
    if (ctx.getFb().getOutputCls()!=null) {
      var output = objectFactory.createOutput(ctx);
      if (outId!=null && !outId.isEmpty()) {
        output.setId(req.outId());
      } else {
        var id = objectFactory.newId(ctx);
        output.setId(id);
//        ctx.setRequest(req.toBuilder()
//          .outId(id)
//          .build());
      }
      ctx.setOutput(output);
    }
    return Uni.createFrom().item(ctx);
  }
}
