package org.hpcclab.msc.object.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.repository.OaasObjectRepository;
import org.hpcclab.msc.object.service.ContextLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FunctionRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRouter.class);

  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  LogicalFunctionHandler logicalFunctionHandler;
  @Inject
  MacroFunctionHandler macroFunctionHandler;
  @Inject
  TaskFunctionHandler taskFunctionHandler;
  @Inject
  ContextLoader contextLoader;


  public Uni<OaasObject> functionCall(FunctionExecContext context) {
    var type = context.getFunction().getType();
    return switch (type) {
      case LOGICAL -> {
        var newObj = logicalFunctionHandler.call(context);
        yield objectRepo.persist(newObj);
      }
      case TASK -> taskFunctionHandler.call(context);
      case MACRO -> macroFunctionHandler.call(context);
    };
  }

  public Uni<OaasObject> reactiveCall(FunctionCallRequest request) {
    return contextLoader.loadCtx(request)
      .invoke(ctx -> ctx.setReactive(true))
      .invoke(this::validate)
      .flatMap(this::functionCall);
  }

  public Uni<OaasObject> activeCall(FunctionCallRequest request) {
    return contextLoader.loadCtx(request)
      .invoke(ctx -> ctx.setReactive(false))
      .invoke(this::validate)
      .flatMap(this::functionCall);
  }


  public void validate(FunctionExecContext context) {
    if (context.getFunction().getName().startsWith("builtin.logical")) {
      logicalFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==OaasFunction.FuncType.MACRO) {
      macroFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==OaasFunction.FuncType.TASK) {
      taskFunctionHandler.validate(context);
    }
  }
}
