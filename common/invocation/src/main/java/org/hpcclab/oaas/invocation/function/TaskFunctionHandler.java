package org.hpcclab.oaas.invocation.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.repository.OaasObjectFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFunctionHandler implements FunctionHandler {

  OaasObjectFactory objectFactory;

  @Inject
  public TaskFunctionHandler(OaasObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public void validate(FunctionExecContext context) {
    if (context.getBinding().getOutputCls() != null && context.getOutputCls()==null)
      throw FunctionValidationException.format(
        "Cannot call function('%s') because outputCls('%s') is not exist",
        context.getFunction().getKey(),
        context.getBinding().getOutputCls()
      );
  }

  public Uni<FunctionExecContext> apply(FunctionExecContext ctx) {
    if (ctx.getBinding().getOutputCls()!=null) {
      var output = objectFactory.createOutput(ctx);

      var rootCtx = ctx;
      while (rootCtx.getParent()!=null) {
        rootCtx = rootCtx.getParent();
      }
      ctx.setOutput(output);
    }
    return Uni.createFrom().item(ctx);
  }
}
