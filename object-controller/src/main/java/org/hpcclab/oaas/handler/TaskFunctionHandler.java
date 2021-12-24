package org.hpcclab.oaas.handler;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.repository.IfnpOaasObjectRepository;
import org.hpcclab.oaas.service.StorageAllocator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFunctionHandler {

  @Inject
  IfnpOaasObjectRepository objectRepo;
  @Inject
  StorageAllocator storageAllocator;

  public void validate(FunctionExecContext context) {
    var main = context.getMain();
    var func = context.getFunction();
    var access = context.getFunctionAccess();

    if (context.getEntry()==main && access!=FunctionAccessModifier.PUBLIC) {
      throw new NoStackException("The object(id='%s') has a function(name='%s') without PUBLIC access"
        .formatted(main.getId(), func.getName())
      );
    }

    if (!context.isReactive()) {
      if (context.getOutputCls().getStateType()
        ==OaasObjectState.StateType.SEGMENTABLE) {
        throw new FunctionValidationException("Can not execute actively the function with the output as segmentable resource type");
      }
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
//    if (func.getTask().getOutputFileNames()!=null) {
//      output.getState().setKeys(func.getTask().getOutputFileNames());
//    }
    output.getState().setKeys(
      ctx.getOutputCls().getStateSpec().getKeys()
    );

    storageAllocator.allocate(output);

    var rootCtx = ctx;
    while (rootCtx.getParent() != null) {
//      rootCtx.getTaskOutputs().add(output);
      rootCtx = rootCtx.getParent();
    }
    rootCtx.getTaskOutputs().add(output);
    ctx.setOutput(output);
    return objectRepo.persistAsync(output)
      .replaceWith(ctx);
  }
}
