package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
  @ProtoField(6)
  List<String> waitFor = List.of();
  @ProtoField(value = 7, defaultValue = "false")
  boolean initWaitFor = false;

  public ObjectStatus() {
  }


  @ProtoFactory
  public ObjectStatus(TaskStatus taskStatus, long createdTime, long submittedTime, long completionTime, String debugLog, List<String> waitFor, boolean initWaitFor) {
    this.taskStatus = taskStatus;
    this.createdTime = createdTime;
    this.submittedTime = submittedTime;
    this.completionTime = completionTime;
    this.debugLog = debugLog;
    this.waitFor = waitFor;
    this.initWaitFor = initWaitFor;
  }

  public void set(TaskCompletion taskCompletion) {
    taskStatus = taskCompletion.getStatus();
    completionTime = taskCompletion.getCompletionTime();
    debugLog = taskCompletion.getDebugLog();
  }

  public void initWaitFor() {
    initWaitFor = true;
    if (waitFor == null) waitFor = Lists.mutable.empty();
  }
}
