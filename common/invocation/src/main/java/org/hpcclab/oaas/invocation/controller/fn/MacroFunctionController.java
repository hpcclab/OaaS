package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.dataflow.DataflowOrchestrator;
import org.hpcclab.oaas.invocation.dataflow.DataflowSemantic;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
public class MacroFunctionController extends AbstractFunctionController {
  final DataflowOrchestrator orchestrator;
  DataflowSemantic semantic;

  public MacroFunctionController(IdGenerator idGenerator,
                                 ObjectMapper mapper,
                                 DataflowOrchestrator orchestrator) {
    super(idGenerator, mapper);
    this.orchestrator = orchestrator;
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
    ensureOutId(ctx);
    return orchestrator.execute(ctx, semantic);
  }

  void ensureOutId(final InvocationCtx ctx) {
    String outId = ctx.getRequest().outId();
    if (outId == null || outId.isEmpty()) return;
    String outputNode = function.getMacro()
      .output();
    if (outputNode != null)
      ctx.getMacroIds().put(outputNode, outId);
  }
}
