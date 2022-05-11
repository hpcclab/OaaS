package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasDataflow;
import org.hpcclab.oaas.model.function.OaasFunctionType;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.hpcclab.oaas.repository.OaasObjectRepository;
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
  OaasObjectFactory objectFactory;


  public void validate(FunctionExecContext context) {
//    if (context.getMainCls().getObjectType()!=OaasObjectType.COMPOUND)
//      throw new FunctionValidationException("Object must be COMPOUND");
    if (context.getFunction().getType()!=OaasFunctionType.MACRO)
      throw new FunctionValidationException("Function must be MACRO");
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
    return execWorkflow(context, func.getMacro())
      .chain(() -> {
        var output = export(func.getMacro(), context);
        return objectRepo.persistAsync(output);
      })
      .map(context::setOutput);
  }

  private OaasObject export(OaasDataflow dataflow,
                            FunctionExecContext ctx) {
    if (dataflow.getExport() != null) {
      return ctx.getWorkflowMap()
        .get(dataflow.getExport());
    } else {
      var output = objectFactory.createOutput(ctx);
      var mem = dataflow.getExports()
        .stream()
        .map(export -> new ObjectReference()
          .setName(export.getAs())
          .setObject(ctx.getWorkflowMap()
            .get(export.getFrom()).getId()))
        .collect(Collectors.toUnmodifiableSet());
      output.setRefs(mem);
      return output;
    }
  }

  private Uni<Void> execWorkflow(FunctionExecContext context,
                                 OaasDataflow workflow) {
    return Multi.createFrom().iterable(workflow.getSteps())
      .call(step -> {
        LOGGER.trace("Execute step {}", step);
        return contextLoader.loadCtxAsync(context, step)
          .flatMap(newCtx -> router.functionCall(newCtx))
          .invoke(newCtx ->
            context.getWorkflowMap().put(step.getAs(), newCtx.getOutput()));
      })
      .collect().last()
      .map(l -> null);
  }
}
