package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.collections.api.set.MutableSet;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.dataflow.DataflowOrchestrator;
import org.hpcclab.oaas.invocation.dataflow.DataflowSemantic;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.invocation.InvocationChain;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pawissanutt
 */
public class MacroFunctionController extends AbstractFunctionController {
  DataflowSemantic semantic;
  final DataflowOrchestrator orchestrator;

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
    return orchestrator.execute(ctx, semantic);
  }
}
