package org.hpcclab.oaas.handler;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.entity.FunctionExecContext;
import org.hpcclab.oaas.repository.OaasObjectRepository;
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
    if (context.getEntry()==main && binding.getAccess()!=FunctionAccessModifier.PUBLIC) {
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

  public Uni<FunctionExecContext> call(FunctionExecContext context) {
    var func = context.getFunction();
    var output = OaasObject.createFromClasses(func.getOutputCls());
    output.setOrigin(context.createOrigin());
    if (func.getTask().getOutputFileNames()!=null) {
      output.getState().setFiles(func.getTask().getOutputFileNames());
    }

    var resUni = objectRepo.persist(output)
      .invoke(o -> storageAllocator.allocate(o));

    var rootCtx = context;
    while (rootCtx.getParent() != null) {
//      rootCtx.getTaskOutputs().add(output);
      rootCtx = rootCtx.getParent();
    }
    rootCtx.getTaskOutputs().add(output);
    return resUni.map(context::setOutput);
  }
}
