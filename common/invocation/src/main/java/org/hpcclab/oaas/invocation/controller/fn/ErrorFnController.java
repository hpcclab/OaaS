package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.invocation.InvocationStatus;

/**
 * @author Pawissanutt
 */
public class ErrorFnController
  extends AbstractFunctionController {
  final String error;
  public ErrorFnController(String error) {
    super(null, new ObjectMapper());
    this.error = error;
  }

  @Override
  public void bind(MetricFactory metricFactory, FunctionBinding functionBinding, OFunction function, OClass cls, OClass outputCls) {
    this.metricFactory = metricFactory;
    this.functionBinding = functionBinding;
    this.function = function;
    this.cls = cls;
    this.outputCls = outputCls;
  }

  @Override
  protected void validate(InvocationCtx ctx) {

  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    ctx.initLog().setStatus(InvocationStatus.FAILED);
    ObjectNode objectNode = mapper.createObjectNode();
    objectNode.put("msg", "The function controller is failed to initiate");
    objectNode.put("reason", error);
    ctx.setRespBody(objectNode);
    return Uni.createFrom().item(ctx);
  }
}
