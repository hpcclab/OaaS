package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.DataflowSemantic;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.invocation.InvocationChain;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class MacroFunctionController extends AbstractFunctionController {
  DataflowSemantic semantic;

  public MacroFunctionController(IdGenerator idGenerator, ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  @Override
  protected void afterBind() {
    semantic = DataflowSemantic.construct(getFunction().getMacro());
  }

  @Override
  protected void validate(InvocationCtx ctx) {

  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    var root = semantic.getRootNode();
    MutableSet<DataflowSemantic.DataflowNode> next = root.getNext();
    InvocationChain[] step2Chain = new InvocationChain[semantic.getAllNode().size()];
    List<InvocationChain> chains = new ArrayList<>();
    for (DataflowSemantic.DataflowNode node : next) {
      chains.add(createChain(node, ctx, step2Chain));
    }
    ctx.setChains(chains);
    for (InvocationChain chain : step2Chain) {
      if (chain==null)
        ctx.getMacroInvIds().add("");
      else
        ctx.getMacroInvIds().add(chain.invId());
    }
    return Uni.createFrom().item(ctx);
  }

  InvocationChain createChain(DataflowSemantic.DataflowNode node,
                              InvocationCtx ctx,
                              InvocationChain[] step2Chain) {
    var i = node.getStepIndex();
    DataflowStep step = node.getStep();
    ObjectTarget objectTarget = resolveId(node, ctx, step2Chain);
    var invId = idGenerator.generate();
    var outId = idGenerator.generate();
    ctx.getMacroIds().put(step.as(), outId);
    InvocationChain.InvocationChainBuilder builder = InvocationChain.builder()
      .invId(invId)
      .outId(outId)
      .main(objectTarget.id)
      .cls(objectTarget.clsOrDefault(step.targetCls()))
      .args(step.args())
      .fb(step.function());
    var chains = new ArrayList<InvocationChain>();
    builder.chains(chains);
    var req = builder.build();
    step2Chain[i] = req;
    for (DataflowSemantic.DataflowNode nextNode : node.getNext()) {
      if (nextNode.getRequire().size() > 1)
        continue;
      var nextChain = createChain(nextNode, ctx, step2Chain);
      chains.add(nextChain);
    }

    return req;
  }

  ObjectTarget resolveId(DataflowSemantic.DataflowNode node,
                         InvocationCtx ctx,
                         InvocationChain[] step2Chain) {
    int stepIndex = node.getMainRefStepIndex();
    if (stepIndex < -1) {
      return ObjectTarget.NULL;
    } else if (stepIndex==-1) {
      return new ObjectTarget(ctx.getRequest().main(), ctx.getRequest().cls());
    } else if (step2Chain[stepIndex] != null) {
      var chain = step2Chain[stepIndex];
      return new ObjectTarget(chain.outId(), null);
    } else {
      return ObjectTarget.NULL;
    }
  }

  record ObjectTarget(String id, String cls) {
    static ObjectTarget NULL = new ObjectTarget(null, null);

    String clsOrDefault(String defaultCls){
      return cls == null? defaultCls: cls;
    }
  }

}
