package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.SimpleStateOperation;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.BuiltinFunctionController;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class UpdateFnController extends AbstractFunctionController
  implements BuiltinFunctionController {
  private static final Logger logger = LoggerFactory.getLogger(UpdateFnController.class);

  boolean merge = false;

  public UpdateFnController(IdGenerator idGenerator,
                            ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  @Override
  protected void afterBind() {
    merge = customConfig.getBoolean("merge", false);
    logger.debug("cls '{}', fb '{}' use merge={}",
      cls, functionBinding.getName(), merge);
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
    if (merge) {
      ObjectNode node = update.getData().getNode();
      node.setAll(body.getNodeOrEmpty());
      update.setData(new JsonBytes(node));
    } else {
      update.setData(body);
    }
    SimpleStateOperation op = functionBinding.isImmutable() ?
      SimpleStateOperation.createObjs(update, cls):
      SimpleStateOperation.updateObjs(update, cls);
    ctx.setStateOperations(List.of(
      op));
    return Uni.createFrom().item(ctx);
  }

  @Override
  public String getFnKey() {
    return "builtin.update";
  }
}
