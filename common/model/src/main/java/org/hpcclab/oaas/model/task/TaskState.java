package org.hpcclab.oaas.model.task;

import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
public class TaskState {
  Set<String> nextTasks;
  Set<String> prevTasks;
  Set<String> completedPrevTasks;
  boolean complete = false;
  boolean submitted = false;

  public TaskState() {
  }

  @ProtoFactory
  public TaskState(Set<String> nextTasks, Set<String> prevTasks, Set<String> completedPrevTasks, boolean complete, boolean submitted) {
    this.nextTasks = nextTasks;
    this.prevTasks = prevTasks;
    this.completedPrevTasks = completedPrevTasks;
    this.complete = complete;
    this.submitted = submitted;
  }

  @ProtoField(1)
  public Set<String> getNextTasks() {
    return nextTasks;
  }

  @ProtoField(2)
  public Set<String> getPrevTasks() {
    return prevTasks;
  }

  @ProtoField(3)
  public Set<String> getCompletedPrevTasks() {
    return completedPrevTasks;
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
