package org.hpcclab.oaas.model.task;

import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Set;

@Data
@Accessors(chain = true)
public class TaskState {
  @ProtoField(1)
  Set<String> nextTasks;
  @ProtoField(2)
  Set<String> prqTasks;
  @ProtoField(3)
  Set<String> completedPrqTasks;
  @ProtoField(4)
  TaskStatus status = TaskStatus.LAZY;
  @ProtoField(value = 5, defaultValue = "-1")
  long submitTime = -1;
  @ProtoField(value = 6, defaultValue = "-1")
  long startTime = -1;
  @ProtoField(value = 7 , defaultValue = "-1")
  long completionTime = -1;
  @ProtoField(8)
  String debugLog;

  public TaskState() {
  }

  @ProtoFactory
  public TaskState(Set<String> nextTasks, Set<String> prqTasks, Set<String> completedPrqTasks, TaskStatus status, long submitTime, long startTime, long completionTime, String debugLog) {
    this.nextTasks = nextTasks;
    this.prqTasks = prqTasks;
    this.completedPrqTasks = completedPrqTasks;
    this.status = status;
    this.submitTime = submitTime;
    this.startTime = startTime;
    this.completionTime = completionTime;
    this.debugLog = debugLog;
  }

  public boolean isCompleted() {
    return status.isCompleted();
  }

  public boolean isSubmitted() {
    return status.isSubmitted();
  }

  public void update(TaskCompletion completion) {
//    status = completion.getStatus();
//    startTime = completion.startTime;
//    completionTime = completion.completionTime;
//    debugLog = completion.debugLog;
  }
}
