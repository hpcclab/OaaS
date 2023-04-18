package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UnifiedFunctionRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedFunctionRouter.class);

  LogicalFunctionApplier logicalFunctionApplier;
  MacroFunctionApplier macroFunctionApplier;
  TaskFunctionApplier taskFunctionApplier;
  ContextLoader contextLoader;

  @Inject
  public UnifiedFunctionRouter(LogicalFunctionApplier logicalFunctionHandler,
                               MacroFunctionApplier macroFunctionHandler,
                               TaskFunctionApplier taskFunctionHandler,
                               ContextLoader contextLoader) {
    this.logicalFunctionApplier = logicalFunctionHandler;
    this.macroFunctionApplier = macroFunctionHandler;
    this.taskFunctionApplier = taskFunctionHandler;
    this.contextLoader = contextLoader;
    this.macroFunctionApplier.setSubFunctionApplier(this::apply);
  }


  public Uni<InvApplyingContext> apply(InvApplyingContext context) {
    var type = context.getFunction().getType();
    return switch (type) {
      case LOGICAL -> logicalFunctionApplier.apply(context);
      case TASK, IM_TASK -> taskFunctionApplier.apply(context);
      case MACRO -> macroFunctionApplier.apply(context);
      default -> throw StdOaasException.notImplemented();
    };
  }

  public Uni<InvApplyingContext> apply(ObjectAccessLanguage request) {
    return contextLoader.loadCtxAsync(request)
      .invoke(this::validate)
      .flatMap(this::apply);
  }

  public Uni<InvApplyingContext> apply(InvocationRequest request) {
    return contextLoader.loadCtxAsync(request)
      .invoke(this::validate)
      .flatMap(this::apply);
  }


  public void validate(InvApplyingContext context) {
    var main = context.getMain();
    var func = context.getFunction();
    var access = context.getBinding().getAccess();

    if (context.getEntry()==main && access!=FunctionAccessModifier.PUBLIC) {
      throw FunctionValidationException.accessError(main.getId(), func.getKey());
    }

    if (context.getFunction().getType()==FunctionType.LOGICAL) {
      logicalFunctionApplier.validate(context);
    }
    if (context.getFunction().getType()==FunctionType.MACRO) {
      macroFunctionApplier.validate(context);
    }
    if (context.getFunction().getType()==FunctionType.TASK) {
      taskFunctionApplier.validate(context);
    }
  }
}
