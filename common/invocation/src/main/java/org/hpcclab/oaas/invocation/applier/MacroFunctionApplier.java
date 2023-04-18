package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.MacroConfig;
import org.hpcclab.oaas.model.invocation.DataflowGraph;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.repository.OaasObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class MacroFunctionApplier implements FunctionApplier {
  private static final Logger logger = LoggerFactory.getLogger(MacroFunctionApplier.class);

  ContextLoader contextLoader;
  OaasObjectFactory objectFactory;

  Function<InvApplyingContext, Uni<InvApplyingContext>> subFunctionApplier;

  @Inject
  public MacroFunctionApplier(ContextLoader contextLoader,
                              OaasObjectFactory objectFactory) {
    this.contextLoader = contextLoader;
    this.objectFactory = objectFactory;
  }

  public void setSubFunctionApplier(Function<InvApplyingContext, Uni<InvApplyingContext>> subFunctionApplier) {
    this.subFunctionApplier = subFunctionApplier;
  }

  public void validate(InvApplyingContext context) {
    if (context.getFunction().getType()!=FunctionType.MACRO)
      throw new FunctionValidationException("Function must be MACRO");
  }

  private void setupMap(InvApplyingContext ctx) {
    Map<String, OaasObject> map = new HashMap<>();
    ctx.setWorkflowMap(map);
    map.put("$self", ctx.getMain());
    for (int i = 0; i < ctx.getInputs().size(); i++) {
      map.put("$" + i, ctx.getInputs().get(i));
    }
  }

  public Uni<InvApplyingContext> apply(InvApplyingContext ctx) {
    validate(ctx);
    setupMap(ctx);
    ctx.setDataflowGraph(new DataflowGraph(ctx));
    var func = ctx.getFunction();
    return applyDataflow(ctx, func.getMacro())
      .map(ignored -> {
        var output = export(ctx, func.getMacro());
        ctx.setOutput(output);
        return ctx;
      });
  }

  private OaasObject export(InvApplyingContext ctx,
                            MacroConfig dataflow) {
    if (dataflow.getExport()!=null) {
      return ctx.getWorkflowMap()
        .get(dataflow.getExport());
    } else {
      var output = objectFactory.createOutput(ctx);
      var mem = dataflow.getExports()
        .stream()
        .map(export -> new ObjectReference()
          .setName(export.getAs())
          .setObjId(ctx.getWorkflowMap()
            .get(export.getFrom()).getId()))
        .collect(Collectors.toUnmodifiableSet());
      output.setRefs(mem);
      return output;
    }
  }

  private Uni<List<InvApplyingContext>> applyDataflow(InvApplyingContext context,
                                                      MacroConfig workflow) {
    var request = context.getRequest();
    return Multi.createFrom().iterable(workflow.getSteps())
      .onItem().transformToUniAndConcatenate(step -> {
        return loadSubContext(context, step)
          .flatMap(newCtx -> subFunctionApplier.apply(newCtx))
          .invoke(newCtx -> {
            if (newCtx.getOutput()!=null
              && step.getAs()!=null
              && request!=null
              && request.macroIds().containsKey(step.getAs()))
              newCtx.getOutput().setId(request.macroIds().get(step.getAs()));
            context.getWorkflowMap().put(step.getAs(), newCtx.getOutput());
          });
      })
      .collect().asList()
      .invoke(ctxList -> {
        for (var ctx : ctxList) {
          context.addTaskOutput(ctx.getOutput());
        }
      });
  }


  public Uni<InvApplyingContext> loadSubContext(InvApplyingContext baseCtx,
                                                DataflowStep step) {
//    logger.debug("loadSubContext {}", step.getAs());
    var newCtx = new InvApplyingContext();
    newCtx.setParent(baseCtx);
    newCtx.setArgs(step.getArgs());
    if (step.getArgRefs()!=null && !step.getArgRefs().isEmpty()) {
      var map = new HashMap<String, String>();
      Map<String, String> baseArgs = baseCtx.getArgs();
      if (baseArgs!=null) {
        for (var entry : step.getArgRefs().entrySet()) {
          var resolveArg = baseArgs.get(entry.getValue());
          if (resolveArg!=null)
            map.put(entry.getKey(), resolveArg);
        }
      }
      if (newCtx.getArgs()!=null) {
        newCtx.setArgs(Maps.mutable.ofMap(newCtx.getArgs()));
        newCtx.getArgs().putAll(map);
      } else {
        newCtx.setArgs(map);
      }
    }
    baseCtx.addSubContext(newCtx);
    baseCtx.getDataflowGraph()
      .addNode(newCtx, step);
    return contextLoader.resolveObj(baseCtx, step.getTarget())
      .invoke(newCtx::setMain)
      .map(__ -> contextLoader.loadClsAndFunc(newCtx, step.getFunction()))
      .call(() -> resolveInputs(baseCtx, newCtx, step))
      ;
  }


  private Uni<Void> resolveInputs(InvApplyingContext baseCtx,
                                  InvApplyingContext currentCtx,
                                  DataflowStep step) {
    List<String> inputRefs = step.getInputRefs()==null ? List.of():step.getInputRefs();
    return Multi.createFrom().iterable(inputRefs)
      .onItem().transformToUniAndConcatenate(ref -> contextLoader.resolveObj(baseCtx, ref))
      .collect().asList()
      .invoke(currentCtx::setInputs)
      .replaceWithVoid();
  }


}
