package org.hpcclab.msc.object.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
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


  public Uni<OaasObject> functionCall(FunctionExecContext context) {
    if (context.getFunction().getType()==OaasFunction.Type.LOGICAL) {
      var newObj = logicalFunctionHandler.call(context);
      return objectRepo.persist(newObj);
    }
    if (context.getFunction().getType()==OaasFunction.Type.MACRO) {
      return macroFunctionHandler.call(context);
    }
    if (context.getFunction().getType()==OaasFunction.Type.TASK) {
      return taskFunctionHandler.call(context);
    }
    LOGGER.warn("Receive function with type {} which is not supported", context.getFunction().getType());
    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
  }

  public Uni<OaasObject> reactiveCall(FunctionCallRequest request) {
    return contextLoader.load(request)
      .invoke(ctx -> ctx.setReactive(true))
      .flatMap(this::functionCall);
  }

  public Uni<OaasObject> activeCall(FunctionCallRequest request) {
    return contextLoader.load(request)
      .invoke(ctx -> ctx.setReactive(false))
      .flatMap(this::functionCall);
  }


  public void validate(FunctionExecContext context) {
    if (context.getFunction().getName().startsWith("builtin.logical")) {
      logicalFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==OaasFunction.Type.MACRO) {
      macroFunctionHandler.validate(context);
    }
    if (context.getFunction().getType()==OaasFunction.Type.TASK) {
      taskFunctionHandler.validate(context);
    }
  }
}
