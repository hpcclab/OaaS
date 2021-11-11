package org.hpcclab.oaas.handler;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.model.function.FunctionCallRequest;
import org.hpcclab.oaas.entity.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.task.TaskExecRequest;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.service.ContextLoader;
import org.hpcclab.oaas.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

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
  TaskExecutionService resourceRequestService;
  @Inject
  ContextLoader contextLoader;


  public Uni<FunctionExecContext> functionCall(FunctionExecContext context) {
    LOGGER.debug("functionCall context.function {}", context.getFunction());
    var type = context.getFunction().getType();
    return switch (type) {
      case LOGICAL -> logicalFunctionHandler.call(context);
      case TASK -> taskFunctionHandler.call(context);
      case MACRO -> macroFunctionHandler.call(context);
    };
  }

  @ReactiveTransactional
  public Uni<FunctionExecContext> functionCall(FunctionCallRequest request,
                                               boolean reactive) {

    return contextLoader.loadCtx(request)
      .invoke(ctx -> ctx.setReactive(reactive))
      .invoke(this::validate)
      .flatMap(this::functionCall);

  }

  public Uni<OaasObject> reactiveCall(FunctionCallRequest request) {
    return functionCall(request, true)
      .map(FunctionExecContext::getOutput);
  }

  public Uni<OaasObject> activeCall(FunctionCallRequest request) {
    return functionCall(request, false)
      .call(ctx -> Multi.createFrom()
        .iterable(ctx.getTaskOutputs())
        .onItem().transformToUniAndMerge(obj -> {
          var taskExecRequest = new TaskExecRequest()
            .setId(obj.getId().toString())
            .setOriginList(List.of(
              Map.of(obj.getId().toString(), obj.getOrigin())
            ));
          LOGGER.debug("Submit task[id='{}']", obj.getId());
          return resourceRequestService.request(taskExecRequest);
        })
        .collect().last()
      )
      .map(FunctionExecContext::getOutput);
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
