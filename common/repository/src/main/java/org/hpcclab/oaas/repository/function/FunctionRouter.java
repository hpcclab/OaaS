package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FunctionRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRouter.class);

  LogicalFunctionHandler logicalFunctionHandler;
  MacroFunctionHandler macroFunctionHandler;
  TaskFunctionHandler taskFunctionHandler;
  ContextLoader contextLoader;

  @Inject
  public FunctionRouter(LogicalFunctionHandler logicalFunctionHandler,
                        MacroFunctionHandler macroFunctionHandler,
                        TaskFunctionHandler taskFunctionHandler,
                        ContextLoader contextLoader) {
    this.logicalFunctionHandler = logicalFunctionHandler;
    this.macroFunctionHandler = macroFunctionHandler;
    this.taskFunctionHandler = taskFunctionHandler;
    this.contextLoader = contextLoader;
  }

  public Uni<FunctionExecContext> invoke(FunctionExecContext context) {
    var type = context.getFunction().getType();
    return switch (type) {
      case LOGICAL -> logicalFunctionHandler.call(context);
      case TASK -> taskFunctionHandler.call(context);
      case MACRO -> macroFunctionHandler.call(context);
    };
  }

//  public Uni<FunctionExecContext> functionCallBlocking(ObjectAccessLangauge request) {
//    var ctx = cachedCtxLoader.loadCtx(request);
//    validate(ctx);
//    return functionCall(ctx);
//  }

  public Uni<FunctionExecContext> invoke(ObjectAccessLangauge request) {
    return contextLoader.loadCtxAsync(request)
      .invoke(this::validate)
      .flatMap(this::invoke);
  }


  public void validate(FunctionExecContext context) {
    if (context.getFunction().getType()==OaasFunctionType.LOGICAL) {
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
