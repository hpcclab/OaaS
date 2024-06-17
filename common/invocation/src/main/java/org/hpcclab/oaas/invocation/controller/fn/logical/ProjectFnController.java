package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.LogicalFunctionController;
import org.hpcclab.oaas.invocation.transform.ODataTransformer;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class ProjectFnController
  extends AbstractFunctionController
  implements LogicalFunctionController {

  public ProjectFnController(IdGenerator idGenerator, ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  List<Dataflows.Transformation> transforms;
  ODataTransformer transformer;

  @Override
  protected void afterBind() {
    JsonNode node = customConfig.get("transform");
    if (node!= null) {
      try {
        transforms = mapper.treeToValue(node, new TypeReference<List<Dataflows.Transformation>>() {
        });
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    } else {
      transforms = List.of();
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
    return "builtin.logical.project";
  }
}
