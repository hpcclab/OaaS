package org.hpcclab.oaas.handler;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.entity.state.OaasObjectState;
import org.hpcclab.oaas.exception.FunctionValidationException;
import org.hpcclab.oaas.exception.NoStackException;
import org.hpcclab.oaas.model.FunctionExecContext;
import org.hpcclab.oaas.model.ObjectResourceRequest;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.service.ResourceRequestService;
import org.hpcclab.oaas.service.StorageAllocator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.stream.Stream;

@ApplicationScoped
public class TaskFunctionHandler {

  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  StorageAllocator storageAllocator;
  @Inject
  ResourceRequestService resourceRequestService;

  public void validate(FunctionExecContext context) {
    var main = context.getMain();
    var func = context.getFunction();
    var bindingOptional = Stream.concat(
        main.getFunctions().stream(),
        main.getCls().getFunctions().stream()
      ).filter(b -> b.getFunction().getName()
        .equals(context.getFunction().getName()))
      .findFirst();

    if (bindingOptional.isEmpty()) {
      throw new NoStackException("No function with name '%s' available in object '%s'"
        .formatted(context.getFunction().getName(), context.getMain().getId())
      );
    }

    var binding = bindingOptional.get();
    if (context.getEntry()==main && binding.getAccess()!=OaasFunctionBinding.AccessModifier.PUBLIC) {
      throw new NoStackException("The object(id='%s') has a function(name='%s') without PUBLIC access"
        .formatted(main.getId(), func.getName())
      );
    }

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
//    storageAllocator.allocate(output);

    var resUni = objectRepo.persist(output)
      .invoke(o -> storageAllocator.allocate(o));

    if (!context.isReactive()) {
      var request = new ObjectResourceRequest()
        .setOwnerObjectId(output.getId().toString())
        .setRequestFile("*");
      resUni = resUni.call(o -> resourceRequestService.request(request));
    }
    return resUni;
  }
}
