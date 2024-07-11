package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.BuiltinFunctionController;
import org.hpcclab.oaas.invocation.transform.ODataTransformer;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class ProjectFnController
  extends AbstractFunctionController
  implements BuiltinFunctionController {

  public ProjectFnController(IdGenerator idGenerator, ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  List<Dataflows.Transformation> transforms;
  ODataTransformer transformer;

  @Override
  protected void afterBind() {
    JsonArray array = customConfig.getJsonArray("transforms");
    transforms = Lists.mutable.empty();
    if (array == null) array = new JsonArray();
    for (int i = 0; i < array.size(); i++) {
      var o = array.getJsonObject(i);
      Dataflows.Transformation transformation = o.mapTo(Dataflows.Transformation.class);
      transforms.add(transformation);
    }
    transformer = ODataTransformer.create(transforms);
  }

  @Override
  protected void validate(InvocationCtx ctx) {

  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    if (transforms.isEmpty()) {
      ctx.setRespBody(ctx.getMain().getData());
    } else {
      var out = transformer.transform(ctx.getMain().getData());
      ctx.setRespBody(out);
    }
    var o = ctx.getMain();
    ctx.setOutput(o);
    return Uni.createFrom().item(ctx);
  }

  @Override
  public String getFnKey() {
    return "builtin.project";
  }
}
