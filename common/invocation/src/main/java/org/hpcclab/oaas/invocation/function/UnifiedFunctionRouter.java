package org.hpcclab.oaas.invocation.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class UnifiedFunctionRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedFunctionRouter.class);

  LogicalFunctionHandler logicalFunctionHandler;
  MacroFunctionHandler macroFunctionHandler;
  TaskFunctionHandler taskFunctionHandler;
  ContextLoader contextLoader;

  @Inject
  public UnifiedFunctionRouter(LogicalFunctionHandler logicalFunctionHandler,
                               MacroFunctionHandler macroFunctionHandler,
                               TaskFunctionHandler taskFunctionHandler,
                               ContextLoader contextLoader) {
    this.logicalFunctionHandler = logicalFunctionHandler;
    this.macroFunctionHandler = macroFunctionHandler;
    this.taskFunctionHandler = taskFunctionHandler;
    this.contextLoader = contextLoader;
  }


  public Uni<FunctionExecContext> apply(FunctionExecContext context) {
    var type = context.getFunction().getType();
    return switch (type) {
      case LOGICAL -> logicalFunctionHandler.apply(context);
      case TASK, IM_TASK -> taskFunctionHandler.apply(context);
      case MACRO -> macroFunctionHandler.apply(context);
      default -> throw StdOaasException.notImplemented();
    };
  }

  public Uni<FunctionExecContext> apply(ObjectAccessLanguage request) {
    return contextLoader.loadCtxAsync(request)
      .invoke(this::validate)
      .flatMap(this::apply);
  }


  public void validate(FunctionExecContext context) {
    var main = context.getMain();
    var func = context.getFunction();
    var access = context.getBinding().getAccess();

    if (context.getEntry()==main && access!=FunctionAccessModifier.PUBLIC) {
      throw FunctionValidationException.accessError(main.getId(), func.getKey());
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
