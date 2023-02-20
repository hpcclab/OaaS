package org.hpcclab.oaas.model.invocation;

import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.DataflowStep;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataflowGraph {
  InvApplyingContext top;
  List<InvocationNode> entries;
  List<InvocationNode> all;
  Map<String, InvocationNode> externalDeps;

  public DataflowGraph(InvApplyingContext top) {
    this.top = top;
    this.entries = Lists.mutable.empty();
    this.all = Lists.mutable.empty();

  }
  public void addNode(InvApplyingContext ctx, DataflowStep step){
    var deps = step.getInputRefs() == null?
      Lists.mutable.<String>empty():
      Lists.mutable.ofAll(step.getInputRefs());
    deps.add(step.getTarget());
    var depNodes = deps
      .stream()
      .filter(dep -> !dep.startsWith("#") && !dep.startsWith("$"))
      .map(dep -> all.stream().filter(node-> Objects.equals(node.getAs(), dep)).findFirst().orElseThrow(() -> FunctionValidationException.cannotResolveMacro(dep, null)))
      .toList();
    var node = createNode(ctx,depNodes, step.getAs());
    if (depNodes.isEmpty()) {
      entries.add(node);
    } else {
      for (InvocationNode depNode : depNodes) {
        depNode.addNext(node);
      }
    }
    all.add(node);
  }

  protected InvocationNode createNode(InvApplyingContext ctx, List<InvocationNode> deps, String as) {
    return new InvocationNode(
      ctx,
      deps,
      Lists.mutable.empty(),
      as,
      false
    );
  }

  public InvApplyingContext getTop() {
    return top;
  }

  public List<InvocationNode> getEntries() {
    return entries;
  }

  public List<InvocationNode> getAll() {
    return all;
  }

  public Map<String, InvocationNode> getExternalDeps() {
    return externalDeps;
  }
}
