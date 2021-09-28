package org.hpcclab.msc.object.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.repository.MscObjectRepository;
import org.hpcclab.msc.object.service.ContextLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FunctionRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRouter.class);

  @Inject
  MscObjectRepository objectRepo;
  @Inject
  LogicalFunctionHandler logicalFunctionHandler;
  @Inject
  MacroFunctionHandler macroFunctionHandler;
  @Inject
  TaskFunctionHandler taskFunctionHandler;
  @Inject
  ContextLoader contextLoader;


  public Uni<MscObject> functionCall(FunctionExecContext context) {
    if (context.getFunction().getType()==MscFunction.Type.LOGICAL) {
      var newObj = logicalFunctionHandler.call(context);
      return objectRepo.persist(newObj);
    }
    if (context.getFunction().getType()==MscFunction.Type.MACRO) {
      return macroFunctionHandler.call(context);
    }
    if (context.getFunction().getType()==MscFunction.Type.TASK) {
      return taskFunctionHandler.call(context);
    }
    LOGGER.warn("Receive function with type {} which is not supported", context.getFunction().getType());
    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
  }

  public Uni<MscObject> reactiveCall(FunctionCallRequest request) {
    return contextLoader.load(request)
      .invoke(ctx -> ctx.setReactive(true))
      .flatMap(this::functionCall);
  }

  public Uni<MscObject> activeCall(FunctionCallRequest request) {
    return contextLoader.load(request)
      .invoke(ctx -> ctx.setReactive(false))
      .flatMap(this::functionCall);
  }


  public void validate(FunctionExecContext context) {
    if (context.getFunction().getName().startsWith("builtin.logical")) {
      logicalFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==MscFunction.Type.MACRO) {
      macroFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==MscFunction.Type.TASK) {
      taskFunctionHandler.validate(context);
    }
  }
}
