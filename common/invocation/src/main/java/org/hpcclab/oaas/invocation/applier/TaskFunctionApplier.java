package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TaskFunctionApplier implements FunctionApplier {
  private static final Logger logger = LoggerFactory.getLogger(TaskFunctionApplier.class);

  OaasObjectFactory objectFactory;

  @Inject
  public TaskFunctionApplier(OaasObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public void validate(InvocationContext context) {
    if (context.getFb().getOutputCls()!=null && context.getOutputCls()==null)
      throw FunctionValidationException.format(
        "Cannot call func('%s') because outputCls('%s') is not exist",
        context.getFunction().getKey(),
        context.getFb().getOutputCls()
      );
  }

  public Uni<InvocationContext> apply(InvocationContext ctx) {
    ctx.setImmutable(ctx.getFb().isForceImmutable() || !ctx.getFunction().getType().isMutable());
    var req = ctx.getRequest();
    if (ctx.getFb().getOutputCls()!=null) {
      var output = objectFactory.createOutput(ctx);
      if (req!=null && (req.outId()!=null)) {
        output.setId(req.outId());
      }
      ctx.setOutput(output);
    }
    return Uni.createFrom().item(ctx);
  }
}
