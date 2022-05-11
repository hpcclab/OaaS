package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectStatus {
  @ProtoField(1)
  TaskStatus taskStatus = TaskStatus.IDLE;
  @ProtoField(value = 2, defaultValue = "-1")
  long createdTime = -1;
  @ProtoField(value = 3, defaultValue = "-1")
  long submittedTime = -1;
  @ProtoField(value = 4, defaultValue = "-1")
  long completionTime = -1;
  @ProtoField(5)
  String debugLog;

  public ObjectStatus() {
  }

  @ProtoFactory
  public ObjectStatus(TaskStatus taskStatus, long createdTime, long submittedTime, long completionTime, String debugLog) {
    this.taskStatus = taskStatus;
    this.createdTime = createdTime;
    this.submittedTime = submittedTime;
    this.completionTime = completionTime;
    this.debugLog = debugLog;
  }

  public void set(TaskCompletion taskCompletion) {
    taskStatus = taskCompletion.getStatus();
    completionTime = taskCompletion.getCompletionTime();
    debugLog = taskCompletion.getDebugLog();
  }
}
