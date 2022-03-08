package org.hpcclab.oaas.repository.function.handler;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.repository.OaasObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFunctionHandler {

  @Inject
  OaasObjectRepository objectRepo;

  public void validate(FunctionExecContext context) {
    var main = context.getMain();
    var func = context.getFunction();
    var access = context.getFunctionAccess();

    if (context.getEntry()==main && access!=FunctionAccessModifier.PUBLIC) {
      throw new FunctionValidationException("The object(id='%s') has a function(name='%s') without PUBLIC access"
        .formatted(main.getId(), func.getName())
      );
    }

  }

  public Uni<FunctionExecContext> call(FunctionExecContext ctx) {
    if (ctx.getOutputCls() == null)
      throw new NoStackException(
        "Cannot call function('%s') because outputCls('%s') is not exist"
        .formatted(ctx.getFunction().getName(), ctx.getFunction().getOutputCls())
      );
    var output = OaasObject.createFromClasses(ctx.getOutputCls());
    output.setOrigin(ctx.createOrigin());

    var rootCtx = ctx;
    while (rootCtx.getParent() != null) {
      rootCtx = rootCtx.getParent();
    }
    rootCtx.getTaskOutputs().add(output);
    ctx.setOutput(output);
    return objectRepo.persistAsync(output)
      .replaceWith(ctx);
  }
}
