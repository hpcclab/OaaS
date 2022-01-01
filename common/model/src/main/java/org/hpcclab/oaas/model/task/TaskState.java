package org.hpcclab.oaas.model.task;

import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Set;

@Data
@Accessors(chain = true)
public class TaskState {
  Set<String> nextTasks;
  Set<String> prqTasks;
  Set<String> completedPrqTasks;
  boolean complete = false;
  boolean submitted = false;

  public TaskState() {
  }

  @ProtoFactory
  public TaskState(Set<String> nextTasks, Set<String> prqTasks, Set<String> completedPrqTasks, boolean complete, boolean submitted) {
    this.nextTasks = nextTasks;
    this.prqTasks = prqTasks;
    this.completedPrqTasks = completedPrqTasks;
    this.complete = complete;
    this.submitted = submitted;
  }

  @ProtoField(1)
  public Set<String> getNextTasks() {
    return nextTasks;
  }

  @ProtoField(2)
  public Set<String> getPrqTasks() {
    return prqTasks;
  }

  @ProtoField(3)
  public Set<String> getCompletedPrqTasks() {
    return completedPrqTasks;
  }

  @ProtoField(value = 4, defaultValue = "false")
  public boolean isComplete() {
    return complete;
  }

  @ProtoField(value = 5, defaultValue = "false")
  public boolean isSubmitted() {
    return submitted;
  }
}
