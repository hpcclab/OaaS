package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.BuiltinFunctionController;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
public class GetFnController
  extends AbstractFunctionController
  implements BuiltinFunctionController {
  public GetFnController(IdGenerator idGenerator, ObjectMapper mapper) {
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
    var o = ctx.getMain();
    ctx.setOutput(o);
    return Uni.createFrom().item(ctx);
  }

  @Override
  public String getFnKey() {
    return "builtin.get";
  }
}
