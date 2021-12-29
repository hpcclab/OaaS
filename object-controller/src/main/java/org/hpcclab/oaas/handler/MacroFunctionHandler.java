package org.hpcclab.oaas.handler;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.function.OaasWorkflow;
import org.hpcclab.oaas.model.object.OaasCompoundMember;
import org.hpcclab.oaas.model.object.OaasObjectType;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.service.CachedCtxLoader;
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
  CachedCtxLoader cachedCtxLoader;

  public void validate(FunctionExecContext context) {
    if (context.getMainCls().getObjectType()!=OaasObjectType.COMPOUND)
      throw new NoStackException("Object must be COMPOUND").setCode(400);
    if (context.getFunction().getType()!=OaasFunctionType.MACRO)
      throw new NoStackException("Function must be MACRO").setCode(400);
  }

  private void setupMap(FunctionExecContext ctx) {
    Map<String, OaasObject> map = new HashMap<>();
    ctx.setWorkflowMap(map);
    map.put("$self", ctx.getMain());
    for (int i = 0; i < ctx.getAdditionalInputs().size(); i++) {
      map.put("$" + i, ctx.getAdditionalInputs().get(i));
    }
  }

  public Uni<FunctionExecContext> call(FunctionExecContext context) {
    validate(context);
    setupMap(context);
    var func = context.getFunction();
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("func {}", func);
    var output = OaasObject.createFromClasses(context.getOutputCls());
    output.setOrigin(context.createOrigin());

    return execWorkflow(context, func.getMacro())
      .chain(() -> {
        var mem = func.getMacro().getExports()
          .stream()
          .map(export -> new OaasCompoundMember()
            .setName(export.getAs())
            .setObject(context.getWorkflowMap()
              .get(export.getFrom()).getId()))
          .collect(Collectors.toUnmodifiableSet());
        output.setMembers(mem);
        return objectRepo.persistAsync(output);
      })
      .map(context::setOutput);
  }

  private Uni<Void> execWorkflow(FunctionExecContext context,
                                 OaasWorkflow workflow) {
    return Multi.createFrom().iterable(workflow.getSteps())
      .call(step -> {
//        var target = resolveTarget(context, map, step.getTarget());
//        var inputRefs = step.getInputRefs()
//          .stream()
//          .map(ir -> resolveTarget(context, map, ir))
//          .toList();
        LOGGER.trace("Execute step {}", step);
        return cachedCtxLoader.loadCtx(context, step)
//        return contextLoader.loadCtx(context, target, step)
//          .invoke(newCtx -> newCtx.setAdditionalInputs(inputRefs))
          .flatMap(newCtx -> router.functionCall(newCtx))
          .invoke(newCtx ->
            context.getWorkflowMap().put(step.getAs(), newCtx.getOutput()));
      })
      .collect().last()
      .map(l -> null);
  }
}
