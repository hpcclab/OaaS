package org.hpcclab.oaas.invocation.dataflow;

import lombok.Builder;
import org.hpcclab.oaas.model.function.FunctionExecContext;

import java.util.List;

@Builder(toBuilder = true)
public record InvocationNode(
  FunctionExecContext ctx,
  List<InvocationNode> internalDeps,
  String as,
  Boolean completed
) {
}
