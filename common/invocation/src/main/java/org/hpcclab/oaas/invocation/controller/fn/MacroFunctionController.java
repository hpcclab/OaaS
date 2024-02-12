package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.controller.DataflowSemantic;
import org.hpcclab.oaas.invocation.controller.InvocationCtx;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
@Dependent
public class MacroFunctionController extends AbstractFunctionController {
  DataflowSemantic semantic;
  @Inject
  protected MacroFunctionController(IdGenerator idGenerator, ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  @Override
  protected void afterBind() {
    //TODO
  }

  @Override
  protected void validate(InvocationCtx ctx) {

  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    //TODO
    return null;
  }
}
