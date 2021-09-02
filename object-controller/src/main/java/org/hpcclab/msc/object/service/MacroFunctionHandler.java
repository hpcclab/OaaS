package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.math.NumberUtils;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class MacroFunctionHandler {

  @Inject
  MscObjectRepository objectRepo;
  @Inject
  TaskFunctionHandler taskFunctionHandler;
  @Inject
  ContextLoader contextLoader;

  public void validate(FunctionExecContext context) {
    if (context.getTarget().getType()!=MscObject.Type.COMPOUND)
      throw new NoStackException("Object must be COMPOUND").setCode(400);
    if (context.getFunction().getType()!=MscFunction.Type.MACRO)
      throw new NoStackException("Function must be MACRO").setCode(400);
  }

  private MscObject resolveTarget(FunctionExecContext context, String value) {
    if (NumberUtils.isDigits(value)) {
      var i = Integer.parseInt(value);
      return context.getAdditionalInputs().get(i);
    }
    return context.getMembers().get(value);
  }

  public Uni<MscObject> call(FunctionExecContext context) {
    validate(context);

    var func = context.getFunction();
    var subContexts =
      func.getSubFunctions().entrySet().stream()
          .map(entry ->  {
            var subFunc = entry.getValue();
            var subContext = new FunctionExecContext()
              .setTarget(resolveTarget(context, subFunc.getTarget()))
              .setArgs(context.getArgs())
              .setFunction(context.getSubFunctions().get(subFunc.getFuncName()))
              .setAdditionalInputs(subFunc.getInputRefs().stream()
                .map(ref -> resolveTarget(context, ref))
                .collect(Collectors.toList())
              );
            return Map.entry(entry.getKey(), subContext);
          })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    subContexts.values().forEach(taskFunctionHandler::validate);

    var templateObj = context.getFunction().getOutputTemplate();

    var output = templateObj.toObject();
    output.setOrigin(new MscObjectOrigin(
      context.getTarget(),
      context.getFunction(),
      context.getArgs()
    ));

    return Multi.createFrom().iterable(subContexts.entrySet())
      .onItem().transformToUniAndConcatenate(entry -> taskFunctionHandler
        .subCall(entry.getValue())
        .map(v -> Map.entry(entry.getKey(),v))
      )
      .invoke(entry -> output.getMembers().put(entry.getKey(), entry.getValue().getId()))
      .collect().last()
      .flatMap(l -> objectRepo.persist(output));
  }
}
