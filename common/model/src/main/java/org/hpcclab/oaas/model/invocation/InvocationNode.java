package org.hpcclab.oaas.model.invocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hpcclab.oaas.model.function.FunctionType;

import java.util.List;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
public class InvocationNode{
  InvocationContext ctx;
  List<InvocationNode> internalDeps;
  List<InvocationNode> next;
  String as;
  boolean completed = false;
  boolean marked = false;

  public InvocationNode() {
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public void setMarked(boolean marked) {
    this.marked = marked;
  }

  public void addNext(InvocationNode node) {
    next.add(node);
  }


  public boolean isNested() {
    return ctx.getFunction().getType() == FunctionType.MACRO;
  }

  public boolean isReady() {
    if (marked)
      return false;
    boolean ready = true;
    for (InvocationNode internalDep : internalDeps) {
      ready &= internalDep.isCompleted();
    }
    return ready;
  }

  @Override
  public String toString() {
    return "InvocationNode{" +
      "as='" + as + '\'' +
      ", completed=" + completed +
      ", marked=" + marked +
      '}';
  }
}
