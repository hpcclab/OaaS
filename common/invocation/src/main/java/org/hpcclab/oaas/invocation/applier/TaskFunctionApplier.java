package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFunctionApplier implements FunctionApplier {
  private static final Logger logger = LoggerFactory.getLogger( TaskFunctionApplier.class );

  OaasObjectFactory objectFactory;

  @Inject
  public TaskFunctionApplier(OaasObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public void validate(InvApplyingContext context) {
    if (context.getBinding().getOutputCls() != null && context.getOutputCls()==null)
      throw FunctionValidationException.format(
        "Cannot call function('%s') because outputCls('%s') is not exist",
        context.getFunction().getKey(),
        context.getBinding().getOutputCls()
      );
  }

  public Uni<InvApplyingContext> apply(InvApplyingContext ctx) {
    ctx.setImmutable(ctx.getBinding().isForceImmutable() || !ctx.getFunction().getType().isMutable());
    var req = ctx.getRequest();
    if (ctx.getBinding().getOutputCls()!=null) {
      var output = objectFactory.createOutput(ctx);
      if (req != null)
        output.setId(req.outId());
//      var rootCtx = ctx;
//      while (rootCtx.getParent()!=null) {
//        rootCtx = rootCtx.getParent();
//      }
      ctx.setOutput(output);
    }
    return Uni.createFrom().item(ctx);
  }
}
