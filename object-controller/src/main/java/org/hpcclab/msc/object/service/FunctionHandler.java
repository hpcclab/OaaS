package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceThrowable;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class FunctionHandler {

  @Inject
  MscObjectRepository objectRepo;
  @Inject
  LogicalFunctionHandler buildInFunctionCaller;

  public Uni<MscObject> reactiveFuncCall(MscObject mscObject,
                                         MscFunction function,
                                         Map<String, String> args) {
    var canInvoke = mscObject.getFunctions() == null || mscObject.getFunctions()
      .contains(function.getName());
    if (!canInvoke)
      return Uni.createFrom().failure(new NoStackTraceThrowable("Can not call this function"));

    if (function.getName().startsWith("buildin.logical")) {
      var newObj = buildInFunctionCaller.call(mscObject, function, args);
      return objectRepo.persist(newObj);
    }

    var newObj = new MscObject()
      .setOrigin(new MscObjectOrigin()
        .setFuncName(function.getName())
        .setArgs(args)
        .setParentId(mscObject.getId())
        .setRootId(mscObject.getOrigin().getRootId())
      );
    return objectRepo.persist(newObj);
  }

  public Uni<? extends MscObject> activeFuncCall(MscObject mscObject,
                                                 MscFunction function,
                                                 Map<String, String> args) {
    var canInvoke = mscObject.getFunctions()
      .contains(function.getName());
    if (!canInvoke)
      return Uni.createFrom().failure(new NoStackTraceThrowable("Can not call this function"));

    if (function.getName().startsWith("buildin.logical")) {
      var newObj = buildInFunctionCaller.call(mscObject, function, args);
      return objectRepo.persist(newObj);
    }
    return null;
  }
}
