package org.hpcclab.oaas.model.invocation;

import lombok.Getter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.DataflowStep;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class DataflowGraph {
  InvocationContext ctx;
  List<InternalInvocationNode> entries;
  List<InternalInvocationNode> all;
  Map<String, InternalInvocationNode> externalDeps;

  boolean failed = false;

  public DataflowGraph(InvocationContext ctx) {
    this.ctx = ctx;
    this.entries = Lists.mutable.empty();
    this.all = Lists.mutable.empty();

  }
  public void addNode(InvocationContext ctx, DataflowStep step){
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
      for (InternalInvocationNode depNode : depNodes) {
        depNode.addNext(node);
      }
    }
    all.add(node);
  }

  protected InternalInvocationNode createNode(InvocationContext ctx, List<InternalInvocationNode> deps, String as) {
    return new InternalInvocationNode(
      ctx,
      deps,
      Lists.mutable.empty(),
      as,
      false,
      false
    );
  }

  public Set<InternalInvocationNode> findNextExecutable(boolean marking) {
    Set<InternalInvocationNode> readyNodes = Sets.mutable.empty();
    Deque<InternalInvocationNode> visited = new ArrayDeque<>();
    for (InternalInvocationNode entry : entries) {
      visited.push(entry);
    }
    while (!visited.isEmpty()) {
      var current = visited.pop();
      if (current.isCompleted()) {
        for (InternalInvocationNode invocationNode : current.next) {
          visited.push(invocationNode);
        }
      } else if (current.isReady()) {
        readyNodes.add(current);
        if (marking)
          current.setMarked(true);
      }
    }
    return readyNodes;
  }
  public boolean isAllCompleted() {
    for (InternalInvocationNode node : all) {
      if (!node.isCompleted())
        return false;
    }
    return true;
  }

  public boolean isFail() {
    return failed;
  }

  public void setFailed(boolean failed) {
    this.failed = failed;
  }

  public List<InvocationNode> exportGraph() {
    var l = all.stream()
      .map(InternalInvocationNode::export)
      .collect(Collectors.toList());
    l.add(makeMainNode(l));
    return l;
  }

  public InvocationNode makeMainNode(List<InvocationNode> subNode) {
    return ctx.initNode()
      .setWaitFor(subNode.stream().map(InvocationNode::getKey).collect(Collectors.toSet()));
  }
}
