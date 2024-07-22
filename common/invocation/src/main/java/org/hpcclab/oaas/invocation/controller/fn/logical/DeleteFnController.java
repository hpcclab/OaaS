package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.BuiltinFunctionController;
import org.hpcclab.oaas.invocation.state.DeleteStateOperation;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class DeleteFnController
  extends AbstractFunctionController
  implements BuiltinFunctionController {
  public DeleteFnController(IdGenerator idGenerator, ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  @Override
  protected void validate(InvocationCtx ctx) {
    String main = ctx.getRequest().main();
    if (main== null || main.isBlank())
      throw new InvocationException("function '%s' require main object".formatted(function.getKey()));
  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    ctx.setStateOperations(List.of(new DeleteStateOperation(List.of(ctx.getMain()), cls)));
    return Uni.createFrom().item(ctx);
  }

  @Override
  public String getFnKey() {
    return "builtin.delete";
  }
}
