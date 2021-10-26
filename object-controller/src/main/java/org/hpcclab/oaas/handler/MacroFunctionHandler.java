package org.hpcclab.oaas.handler;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.math.NumberUtils;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasWorkflow;
import org.hpcclab.oaas.entity.object.OaasCompoundMember;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.FunctionExecContext;
import org.hpcclab.oaas.exception.NoStackException;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.service.ContextLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class MacroFunctionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(MacroFunctionHandler.class);

  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  FunctionRouter router;
  @Inject
  ContextLoader contextLoader;
  @Inject
  OaasMapper oaasMapper;

  public void validate(FunctionExecContext context) {
    if (context.getMain().getType()!=OaasObject.ObjectType.COMPOUND)
      throw new NoStackException("Object must be COMPOUND").setCode(400);
    if (context.getFunction().getType()!=OaasFunction.FuncType.MACRO)
      throw new NoStackException("Function must be MACRO").setCode(400);
  }

  private OaasObject resolveTarget(FunctionExecContext context, Map<String, OaasObject> workflowMap, String value) {
    if (value.equals("$self")) return context.getMain();
    if (workflowMap.containsKey(value)) {
      return workflowMap.get(value);
    }
    if (NumberUtils.isDigits(value)) {
      var i = Integer.parseInt(value);
      return context.getAdditionalInputs().get(i);
    }
    return context.getMain().getMembers()
      .stream()
      .filter(cm -> cm.getName().equals(value))
      .findAny().orElseThrow(() -> new NoStackException("Can not resolve '" + value + "'"))
      .getObject();
  }

  public Uni<OaasObject> call(FunctionExecContext context) {
    validate(context);

    var func = context.getFunction();
    LOGGER.info("func {}", Json.encodePrettily(func));
    var output = OaasObject.createFromClasses(context.getFunction().getOutputCls());
    output.setOrigin(new OaasObjectOrigin(context));

    return execWorkflow(context, func.getMacro())
      .flatMap(wfResults -> {
        var mem = func.getMacro().getExports()
          .stream()
          .map(export -> new OaasCompoundMember()
            .setName(export.getAs())
            .setObject(wfResults.get(export.getFrom())))
          .collect(Collectors.toUnmodifiableSet());
        output.setMembers(mem);
        return objectRepo.persistAndFlush(output);
      });
  }

  private Uni<Map<String, OaasObject>> execWorkflow(FunctionExecContext context,
                                                    OaasWorkflow workflow) {
    var map = new HashMap<String, OaasObject>();
    return Multi.createFrom().iterable(workflow.getSteps())
      .call(step -> {
        var target = resolveTarget(context, map, step.getTarget());
        var inputRefs = step.getInputRefs()
          .stream()
          .map(ir -> resolveTarget(context, map, ir))
          .toList();
//        var newCtx = oaasMapper.copy(context)
//          .setFunction(functionBinding.getFunction())
//          .setMain(target)
//          .setAdditionalInputs(inputRefs);
        return contextLoader.loadCtx(context, target, step)
          .invoke(ctx -> ctx.setAdditionalInputs(inputRefs))
          .flatMap(ctx -> router.functionCall(ctx))
          .invoke(obj -> map.put(step.getAs(), obj));
      })
      .collect().last()
      .map(l -> map);
  }
}
