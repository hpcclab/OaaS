package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.dataflow.DataflowSemantic;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.invocation.InvocationChain;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public class ChainFunctionController extends AbstractFunctionController {
  DataflowSemantic semantic;

  public ChainFunctionController(IdGenerator idGenerator, ObjectMapper mapper) {
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
    Set<DataflowSemantic.DataflowNode> next = root.next();
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
    var i = node.stepIndex();
    Dataflows.Step step = node.step();
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
    for (DataflowSemantic.DataflowNode nextNode : node.next()) {
      if (nextNode.require().size() > 1)
        continue;
      var nextChain = createChain(nextNode, ctx, step2Chain);
      chains.add(nextChain);
    }

    return req;
  }

  ObjectTarget resolveId(DataflowSemantic.DataflowNode node,
                         InvocationCtx ctx,
                         InvocationChain[] step2Chain) {
    int stepIndex = node.mainRefStepIndex();
    if (stepIndex < 0) {
      return ObjectTarget.NULL;
    } else if (stepIndex==0) {
      return new ObjectTarget(ctx.getRequest().main(), ctx.getRequest().cls());
    } else if (step2Chain[stepIndex]!=null) {
      var chain = step2Chain[stepIndex];
      return new ObjectTarget(chain.outId(), null);
    } else {
      return ObjectTarget.NULL;
    }
  }

  record ObjectTarget(String id, String cls) {
    static ObjectTarget NULL = new ObjectTarget(null, null);

    String clsOrDefault(String defaultCls) {
      return cls==null ? defaultCls:cls;
    }
  }

}
