package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.DataflowSemantic;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.id.IdGenerator;

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
    MutableMap<Integer, InvocationRequest> step2Req = Maps.mutable.empty();
    for (DataflowSemantic.DataflowNode node : next) {
      ctx.getReqToProduce().add(createRequest(node, ctx, step2Req));
    }
    return Uni.createFrom().item(ctx);
  }

  InvocationRequest createRequest(DataflowSemantic.DataflowNode node,
                                  InvocationCtx ctx,
                                  MutableMap<Integer, InvocationRequest> step2Req) {
    var i = node.getStepIndex();
    DataflowStep step = node.getStep();
    ObjectTarget objectTarget = resolveId(node, ctx, step2Req);
    InvocationRequest.InvocationRequestBuilder builder = InvocationRequest.builder()
      .invId(idGenerator.generate())
      .outId(idGenerator.generate())
      .main(objectTarget.id)
      .cls(objectTarget.cls)
      .args(step.args())
      .fb(step.function())
      ;

    var req = builder.build();
    step2Req.put(i, req);
    return req;
  }

  ObjectTarget resolveId(DataflowSemantic.DataflowNode node,
                   InvocationCtx ctx,
                   MutableMap<Integer, InvocationRequest> step2Req) {
    int stepIndex = node.getMainRefStepIndex();
    if (stepIndex < -1) {
      return ObjectTarget.NULL;
    } else if (stepIndex == -1) {
      return new ObjectTarget(ctx.getRequest().main(), ctx.getRequest().cls());
    } else if (step2Req.containsKey(stepIndex)){
      InvocationRequest request = step2Req.get(stepIndex);
      return new ObjectTarget(request.outId(), null);
    } else {
      return ObjectTarget.NULL;
    }
  }

  record ObjectTarget(String id, String cls){
    static ObjectTarget NULL = new ObjectTarget(null, null);
  }

}
