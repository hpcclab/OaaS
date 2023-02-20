package org.hpcclab.oaas.model.invocation;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder(toBuilder = true)
@Getter
public class InvocationNode{
  InvApplyingContext ctx;
  List<InvocationNode> internalDeps;
  List<InvocationNode> next;
  String as;
  boolean completed = false;

  public InvocationNode() {
  }

  public InvocationNode(InvApplyingContext ctx, List<InvocationNode> internalDeps, List<InvocationNode> next, String as, boolean completed) {
    this.ctx = ctx;
    this.internalDeps = internalDeps;
    this.next = next;
    this.as = as;
    this.completed = completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public void addNext(InvocationNode node) {
    next.add(node);
  }

  @Override
  public String toString() {
    return "InvocationNode{" +
      "internalDeps=" + internalDeps +
      ", next=" + next +
      ", as='" + as + '\'' +
      ", completed=" + completed +
      '}';
  }
}
