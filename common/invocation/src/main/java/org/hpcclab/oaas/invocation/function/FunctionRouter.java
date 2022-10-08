package org.hpcclab.oaas.invocation.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.RepoContextLoader;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.FunctionType;
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
  RepoContextLoader contextLoader;

  @Inject
  public FunctionRouter(LogicalFunctionHandler logicalFunctionHandler,
                        MacroFunctionHandler macroFunctionHandler,
                        TaskFunctionHandler taskFunctionHandler,
                        RepoContextLoader contextLoader) {
    this.logicalFunctionHandler = logicalFunctionHandler;
    this.macroFunctionHandler = macroFunctionHandler;
    this.taskFunctionHandler = taskFunctionHandler;
    this.contextLoader = contextLoader;
  }

  public Uni<FunctionExecContext> apply(FunctionExecContext context) {
    var type = context.getFunction().getType();
    return switch (type) {
      case LOGICAL -> logicalFunctionHandler.apply(context);
      case TASK -> taskFunctionHandler.apply(context);
      case MACRO -> macroFunctionHandler.apply(context);
    };
  }

//  public Uni<FunctionExecContext> functionCallBlocking(ObjectAccessLangauge request) {
//    var ctx = cachedCtxLoader.loadCtx(request);
//    validate(ctx);
//    return functionCall(ctx);
//  }

  public Uni<FunctionExecContext> apply(ObjectAccessLangauge request) {
    return contextLoader.loadCtxAsync(request)
      .invoke(this::validate)
      .flatMap(this::apply);
  }


  public void validate(FunctionExecContext context) {
    var main = context.getMain();
    var func = context.getFunction();
    var access = context.getBinding().getAccess();

    if (context.getEntry()==main && access!=FunctionAccessModifier.PUBLIC) {
      throw FunctionValidationException.accessError(main.getId(), func.getName());
    }

    if (context.getFunction().getType()==FunctionType.LOGICAL) {
      logicalFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==FunctionType.MACRO) {
      macroFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==FunctionType.TASK) {
      taskFunctionHandler.validate(context);
    }
  }
}
