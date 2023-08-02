package org.hpcclab.oaas.model.invocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.hpcclab.oaas.model.function.FunctionType;

import java.util.List;
import java.util.stream.Collectors;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
public class InternalInvocationNode {
  InvocationContext ctx;
  List<InternalInvocationNode> internalDeps;
  List<InternalInvocationNode> next;
  String as;
  boolean completed = false;
  boolean marked = false;

  public InternalInvocationNode() {
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public void setMarked(boolean marked) {
    this.marked = marked;
  }

  public void addNext(InternalInvocationNode node) {
    next.add(node);
  }


  public boolean isNested() {
    return ctx.getFunction().getType() == FunctionType.MACRO;
  }

  public boolean isReady() {
    if (marked)
      return false;
    boolean ready = true;
    for (InternalInvocationNode internalDep : internalDeps) {
      ready &= internalDep.isCompleted();
    }
    return ready;
  }

  public InvocationNode export() {
    var exportNode = ctx.initNode();
    exportNode.setNextInv(next.stream().map(c -> c.getCtx().initNode().getKey())
      .collect(Collectors.toSet()));
    exportNode.setWaitFor(internalDeps.stream().map(c -> c.getCtx().initNode().getKey())
      .collect(Collectors.toSet()));
    return exportNode;
  }

  @Override
  public String toString() {
    return "InternalInvocationNode{" +
      "as='" + as + '\'' +
      ", completed=" + completed +
      ", marked=" + marked +
      ", next=["+next.size()+"]"+
      '}';
  }
}
