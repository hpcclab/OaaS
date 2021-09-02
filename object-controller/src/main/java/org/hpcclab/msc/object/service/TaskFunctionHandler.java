package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFunctionHandler {

  @Inject
  MscObjectRepository objectRepo;

  public void validate(FunctionExecContext context) {
    if (!context.getTarget().getFunctions().contains(context.getFunction().getName()))
      throw new NoStackException("Object(id=%s) Can not be executed by function(name=%s)"
        .formatted(context.getTarget().getId(), context.getFunction().getName()))
        .setCode(400);
  }

  public Uni<MscObject> call(FunctionExecContext context) {
    var func = context.getFunction();
    var output = func.getOutputTemplate().toObject();
    output.setOrigin(new MscObjectOrigin(context));
    return objectRepo.persist(output);
  }

  public Uni<MscObject> subCall(FunctionExecContext context) {
//    throw new NoStackException("Not implemented").setCode(HttpResponseStatus.NOT_IMPLEMENTED.code());
    return call(context);
  }
}
