package org.hpcclab.oaas.invocation.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.exception.NoStackException;
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
    var main = context.getMain();
    var func = context.getFunction();
    var access = context.getBinding().getAccess();

    if (context.getEntry()==main && access!=FunctionAccessModifier.PUBLIC) {
      throw FunctionValidationException.accessError(main.getId(), func.getName());
    }
  }

  public Uni<FunctionExecContext> apply(FunctionExecContext ctx) {
    if (ctx.getOutputCls() == null)
      throw new NoStackException(
        "Cannot call function('%s') because outputCls('%s') is not exist"
        .formatted(ctx.getFunction().getName(), ctx.getFunction().getOutputCls())
      );
    var output = objectFactory.createOutput(ctx);

    var rootCtx = ctx;
    while (rootCtx.getParent() != null) {
      rootCtx = rootCtx.getParent();
    }
//    rootCtx.getTaskOutputs().add(output);
    ctx.setOutput(output);
    return Uni.createFrom().item(ctx);
  }
}
