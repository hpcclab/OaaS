package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.set.MutableSet;
import org.hpcclab.oaas.invocation.controller.DataflowSemantic;
import org.hpcclab.oaas.invocation.InvocationCtx;
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
    for (DataflowSemantic.DataflowNode node : next) {
      ctx.getReqToProduce().add(createRequest(node));
    }
    return Uni.createFrom().item(ctx);
  }

  InvocationRequest createRequest(DataflowSemantic.DataflowNode node) {
    //TODO generate list of requests
    return InvocationRequest.builder().build();
  }
}
