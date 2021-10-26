package org.hpcclab.oaas.handler;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.entity.state.OaasObjectState;
import org.hpcclab.oaas.exception.FunctionValidationException;
import org.hpcclab.oaas.model.FunctionExecContext;
import org.hpcclab.oaas.model.ObjectResourceRequest;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.service.ResourceRequestService;
import org.hpcclab.oaas.service.StorageAllocator;

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
      if (context.getFunction().getOutputCls().getStateType()
        ==OaasObjectState.StateType.SEGMENTABLE) {
        throw new FunctionValidationException("Can not execute actively the function with the output as segmentable resource type");
      }
    }
  }

  public Uni<OaasObject> call(FunctionExecContext context) {
    var func = context.getFunction();
    var output = OaasObject.createFromClasses(func.getOutputCls());
    output.setOrigin(new OaasObjectOrigin(context));
//    if (output.getState().getType() == OaasObjectState.StateType.FILE
//      && output.getState().getFile() == null
//      && context.getMain().getState().getType() == OaasObjectState.StateType.FILE) {
//      output.getState()
//        .setFile(context.getMain().getState().getFile());
//    }
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