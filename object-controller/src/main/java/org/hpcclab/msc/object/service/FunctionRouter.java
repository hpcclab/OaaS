package org.hpcclab.msc.object.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceThrowable;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

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


  public Uni<MscObject> reactiveFuncCall(FunctionCallRequest request) {
    return contextLoader.load(request)
      .flatMap(context -> {


        if (context.getFunction().getName().startsWith("buildin.logical")) {
          var newObj = logicalFunctionHandler.call(context);
          return objectRepo.persist(newObj);
        }
        if (context.getFunction().getType() ==MscFunction.Type.MACRO) {
          return macroFunctionHandler.call(context);
        }
        if (context.getFunction().getType() ==MscFunction.Type.TASK) {
          return taskFunctionHandler.call(context);
        }

        throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
      });
  }

  public Uni<MscObject> activeFuncCall(FunctionCallRequest request) {
    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
  }
}
