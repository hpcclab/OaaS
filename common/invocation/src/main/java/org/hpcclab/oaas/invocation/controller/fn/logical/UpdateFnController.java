package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.SimpleStateOperation;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.LogicalFunctionController;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class UpdateFnController extends AbstractFunctionController
  implements LogicalFunctionController {

  public UpdateFnController(IdGenerator idGenerator,
                            ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  @Override
  protected void validate(InvocationCtx ctx) {

  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    var body = ctx.getRequest().body();
    var main = ctx.getMain();
    var update = main;
    if (functionBinding.isImmutable()) {
      update = main.copy();
      update.getMeta().setId(idGenerator.generate());
    }
    update.setData(body);
    ctx.setStateOperations(List.of(
      SimpleStateOperation.updateObjs(
        List.of(update), cls
      )));
    return Uni.createFrom().item(ctx);
  }

  @Override
  public String getFnKey() {
    return "builtin.logical.update";
  }
}
