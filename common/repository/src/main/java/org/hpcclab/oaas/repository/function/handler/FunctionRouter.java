package org.hpcclab.oaas.repository.function.handler;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.ObjectAccessExpression;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.task.TaskExecRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@RegisterForReflection(targets = {
  TaskExecRequest.class
})
public class FunctionRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRouter.class);

  @Inject
  LogicalFunctionHandler logicalFunctionHandler;
  @Inject
  MacroFunctionHandler macroFunctionHandler;
  @Inject
  TaskFunctionHandler taskFunctionHandler;
  @Inject
  ContextLoader cachedCtxLoader;

  public Uni<FunctionExecContext> functionCall(FunctionExecContext context) {
    var type = context.getFunction().getType();
    return switch (type) {
      case LOGICAL -> logicalFunctionHandler.call(context);
      case TASK -> taskFunctionHandler.call(context);
      case MACRO -> macroFunctionHandler.call(context);
    };
  }

  public Uni<FunctionExecContext> functionCallBlocking(ObjectAccessExpression request) {
    var ctx = cachedCtxLoader.loadCtx(request);
    validate(ctx);
    return functionCall(ctx);
  }
    public Uni<FunctionExecContext> functionCall(ObjectAccessExpression request) {
    return cachedCtxLoader.loadCtxAsync(request)
      .invoke(this::validate)
      .flatMap(this::functionCall);

  }


  public void validate(FunctionExecContext context) {
    if (context.getFunction().getName().startsWith("builtin.logical")) {
      logicalFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==OaasFunctionType.MACRO) {
      macroFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==OaasFunctionType.TASK) {
      taskFunctionHandler.validate(context);
    }
  }
}
