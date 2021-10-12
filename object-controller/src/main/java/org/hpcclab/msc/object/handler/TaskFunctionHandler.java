package org.hpcclab.msc.object.handler;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.object.OaasObjectOrigin;
import org.hpcclab.msc.object.entity.state.OaasObjectState;
import org.hpcclab.msc.object.exception.FunctionValidationException;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.repository.OaasObjectRepository;
import org.hpcclab.msc.object.service.ResourceRequestService;
import org.hpcclab.msc.object.service.StorageAllocator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFunctionHandler {

  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  StorageAllocator storageAllocator;
  @Inject
  ResourceRequestService resourceRequestService;

  public void validate(FunctionExecContext context) {
    if (!context.getMain().getFunctions().contains(context.getFunction().getName()))
      throw new FunctionValidationException(
        "Object(id=%s) Can not be executed by function(name=%s)"
          .formatted(context.getMain().getId(), context.getFunction().getName())
      );
    if (!context.isReactive()) {
      if (context.getFunction().getOutputClass().get(0).getStateType()
        ==OaasObjectState.Type.SEGMENTABLE) {
        throw new FunctionValidationException("Can not execute actively the function with the output as segmentable resource type");
      }
    }
  }

  public Uni<OaasObject> call(FunctionExecContext context) {
    var func = context.getFunction();
    var output = OaasObject.createFromClasses(func.getOutputClass());
    output.setOrigin(new OaasObjectOrigin(context));
    if (output.getState().getType() == OaasObjectState.Type.FILE
      && output.getState().getFile() == null
      && context.getMain().getState().getType() == OaasObjectState.Type.FILE) {
      output.getState()
        .setFile(context.getMain().getState().getFile());
    }
    storageAllocator.allocate(output);
    if (!context.isReactive()) {
      var request = new ObjectResourceRequest()
        .setOwnerObjectId(output.getId().toString())
        .setRequestFile("*");

      return objectRepo.persist(output)
        .call(o -> resourceRequestService.request(request));
    }
    return objectRepo.persist(output);
  }
}
