package org.hpcclab.msc.object.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.repository.MscObjectRepository;
import org.hpcclab.msc.object.service.ContextLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FunctionRouter {

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


  public Uni<MscObject> reactiveCall(FunctionExecContext context) {
    if (context.getFunction().getName().startsWith("buildin.logical")) {
      var newObj = logicalFunctionHandler.call(context);
      return objectRepo.persist(newObj);
    }
    if (context.getFunction().getType()==MscFunction.Type.MACRO) {
      return macroFunctionHandler.call(context);
    }
    if (context.getFunction().getType()==MscFunction.Type.TASK) {
      return taskFunctionHandler.call(context);
    }
    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
  }

  public Uni<MscObject> reactiveCall(FunctionCallRequest request) {
    return contextLoader.load(request)
      .flatMap(this::reactiveCall);
  }

  public Uni<MscObject> activeCall(FunctionCallRequest request) {
    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
  }


  public void validate(FunctionExecContext context) {
    if (context.getFunction().getName().startsWith("buildin.logical")) {
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
