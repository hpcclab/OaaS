package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.MacroSpec;
import org.hpcclab.oaas.model.function.WorkflowExport;
import org.hpcclab.oaas.model.invocation.DataflowGraph;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.invocation.OObjectFactory;
import org.hpcclab.oaas.model.proto.DSMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
public class MacroFunctionApplier implements FunctionApplier {
  private static final Logger logger = LoggerFactory.getLogger(MacroFunctionApplier.class);

  ContextLoader contextLoader;
  OObjectFactory objectFactory;

  Function<InvocationContext, Uni<InvocationContext>> subFunctionApplier;

  @Inject
  public MacroFunctionApplier(ContextLoader contextLoader,
                              OObjectFactory objectFactory) {
    this.contextLoader = contextLoader;
    this.objectFactory = objectFactory;
  }

  public void setSubFunctionApplier(Function<InvocationContext, Uni<InvocationContext>> subFunctionApplier) {
    this.subFunctionApplier = subFunctionApplier;
  }

  public void validate(InvocationContext context) {
    if (context.getFunction().getType()!=FunctionType.MACRO)
      throw new FunctionValidationException("Function must be MACRO");
  }

  private void setupMap(InvocationContext ctx) {
    Map<String, OObject> map = new HashMap<>();
    ctx.setWorkflowMap(map);
    map.put("$self", ctx.getMain());
    for (int i = 0; i < ctx.getInputs().size(); i++) {
      map.put("$" + i, ctx.getInputs().get(i));
    }
  }

  public Uni<InvocationContext> apply(InvocationContext ctx) {
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

  private OObject export(InvocationContext ctx,
                         MacroSpec dataflow) {
    if (dataflow.getExport()!=null) {
      return ctx.getWorkflowMap()
        .get(dataflow.getExport());
    } else {
      var output = objectFactory.createOutput(ctx);
      var refs = new DSMap();
      for (WorkflowExport export : dataflow.getExports()) {
        refs.put(export.getAs(), ctx.getWorkflowMap()
          .get(export.getFrom()).getId());
      }
      output.setRefs(refs);
      return output;
    }
  }

  private Uni<List<InvocationContext>> applyDataflow(InvocationContext context,
                                                     MacroSpec workflow) {
    var request = context.getRequest();
    var macroIds = makeMacroIds(context, workflow);
    return Multi.createFrom().iterable(workflow.getSteps())
      .onItem().transformToUniAndConcatenate(step ->
        loadSubContext(context, step)
          .flatMap(newCtx -> subFunctionApplier.apply(newCtx))
          .invoke(newCtx -> {
            if (newCtx.getOutput()!=null
              && step.getAs()!=null
              && macroIds.containsKey(step.getAs())) {
              newCtx.getOutput().setId(macroIds.get(step.getAs()));
            }

            context.getWorkflowMap().put(step.getAs(), newCtx.getOutput());
          })
      )
      .collect().asList()
      .invoke(ctxList -> {
        for (var ctx : ctxList) {
          context.addTaskOutput(ctx.getOutput());
        }
      });
  }

  private DSMap makeMacroIds(InvocationContext ctx,
                             MacroSpec dataflow) {
    var request = ctx.getRequest();
    var macroIds = request.macroIds();
    if (macroIds == null) {
      macroIds = DSMap.mutable();
      for (DataflowStep step : dataflow.getSteps()) {
        if (step.getAs()==null)
          continue;
        macroIds.put(step.getAs(), objectFactory.newId(ctx));
      }
    }
    return macroIds;
  }


  public Uni<InvocationContext> loadSubContext(InvocationContext baseCtx,
                                               DataflowStep step) {
    var newCtx = new InvocationContext();
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


  private Uni<Void> resolveInputs(InvocationContext baseCtx,
                                  InvocationContext currentCtx,
                                  DataflowStep step) {
    List<String> inputRefs = step.getInputRefs()==null ? List.of():step.getInputRefs();
    return Multi.createFrom().iterable(inputRefs)
      .onItem().transformToUniAndConcatenate(ref -> contextLoader.resolveObj(baseCtx, ref))
      .collect().asList()
      .invoke(currentCtx::setInputs)
      .replaceWithVoid();
  }


}
