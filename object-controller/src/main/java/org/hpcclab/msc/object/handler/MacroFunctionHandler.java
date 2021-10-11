package org.hpcclab.msc.object.handler;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.math.NumberUtils;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.object.OaasObjectOrigin;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.repository.MscObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class MacroFunctionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger( MacroFunctionHandler.class );

  @Inject
  MscObjectRepository objectRepo;
  @Inject
  FunctionRouter router;

  public void validate(FunctionExecContext context) {
    if (context.getMain().getType()!=OaasObject.Type.COMPOUND)
      throw new NoStackException("Object must be COMPOUND").setCode(400);
    if (context.getFunction().getType()!=OaasFunction.Type.MACRO)
      throw new NoStackException("Function must be MACRO").setCode(400);
  }

  private OaasObject resolveTarget(FunctionExecContext context, String value) {
    if (NumberUtils.isDigits(value)) {
      var i = Integer.parseInt(value);
      return context.getAdditionalInputs().get(i);
    }
    return context.getMembers().get(value);
  }

  public Uni<OaasObject> call(FunctionExecContext context) {
    validate(context);

    var func = context.getFunction();
    var subContexts =
      func.getSubFunctions().entrySet().stream()
          .map(entry ->  {
            var subFunc = entry.getValue();
            var subContext = new FunctionExecContext()
              .setMain(resolveTarget(context, subFunc.getTarget()))
              .setArgs(context.getArgs())
              .setFunction(context.getSubFunctions().get(subFunc.getFuncName()))
              .setAdditionalInputs(subFunc.getInputRefs().stream()
                .map(ref -> resolveTarget(context, ref))
                .collect(Collectors.toList())
              );
            return Map.entry(entry.getKey(), subContext);
          })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    subContexts.values().forEach(router::validate);

    var templateObj = context.getFunction().getOutputTemplate();

    var output = templateObj.toObject();
    output.setOrigin(new OaasObjectOrigin(context))
      .setMembers(new HashMap<>());

    return Multi.createFrom().iterable(subContexts.entrySet())
      .onItem().transformToUniAndConcatenate(entry -> router
        .functionCall(entry.getValue())
        .map(v -> Map.entry(entry.getKey(),v))
      )
      .invoke(entry -> output.getMembers().put(entry.getKey(), entry.getValue().getId()))
      .collect().last()
      .flatMap(l -> objectRepo.persist(output))
//      .invoke(o -> LOGGER.info("get output {}", Json.encodePrettily(o)))
      ;
  }
}
