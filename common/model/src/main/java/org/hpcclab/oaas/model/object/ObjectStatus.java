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
  long completedTime = -1;
  @ProtoField(5)
  String debugLog;
  @ProtoField(6)
  List<String> waitFor = List.of();
  @ProtoField(value = 7, defaultValue = "false")
  boolean initWaitFor = false;

  @ProtoField(8)
  String originator;

  public ObjectStatus() {
  }


  @ProtoFactory
  public ObjectStatus(TaskStatus taskStatus, long createdTime, long submittedTime, long completedTime, String debugLog, List<String> waitFor, boolean initWaitFor, String originator) {
    this.taskStatus = taskStatus;
    this.createdTime = createdTime;
    this.submittedTime = submittedTime;
    this.completedTime = completedTime;
    this.debugLog = debugLog;
    this.waitFor = waitFor;
    this.initWaitFor = initWaitFor;
    this.originator = originator;
  }

  public void set(TaskCompletion taskCompletion) {
    if (taskCompletion.isSuccess()) taskStatus = TaskStatus.SUCCEEDED;
    else taskStatus = TaskStatus.FAILED;
    debugLog = taskCompletion.getDebugLog();
    completedTime = System.currentTimeMillis();
  }

  public void initWaitFor() {
    initWaitFor = true;
    if (waitFor == null) waitFor = Lists.mutable.empty();
  }
}
