package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;

@Data
@Accessors(chain = true)
public class ObjectStatus implements Copyable<ObjectStatus> {
  @ProtoField(1)
  TaskStatus taskStatus = TaskStatus.LAZY;
  @ProtoField(value = 2, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long createdTime;
  @ProtoField(value = 3, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long submittedTime;
  @ProtoField(value = 4, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long completedTime;
  @ProtoField(5)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  List<String> waitFor = List.of();
  @ProtoField(value = 6, defaultValue = "false")
  @JsonIgnore
  boolean initWaitFor = false;
  @ProtoField(7)
  @JsonIgnore
  String originator;

  public ObjectStatus() {
  }


  @ProtoFactory
  public ObjectStatus(TaskStatus taskStatus, long createdTime, long submittedTime, long completedTime, List<String> waitFor, boolean initWaitFor, String originator) {
    this.taskStatus = taskStatus;
    this.createdTime = createdTime;
    this.submittedTime = submittedTime;
    this.completedTime = completedTime;
    this.waitFor = waitFor;
    this.initWaitFor = initWaitFor;
    this.originator = originator;
  }

  public ObjectStatus copy() {
    return new ObjectStatus(
      taskStatus,
      createdTime,
      submittedTime,
      completedTime,
      waitFor==null ? null:List.copyOf(waitFor),
      initWaitFor,
      originator
    );
  }

  public void set(TaskCompletion taskCompletion) {
    if (taskCompletion.isSuccess()) taskStatus = TaskStatus.SUCCEEDED;
    else taskStatus = TaskStatus.FAILED;
    completedTime = System.currentTimeMillis();
  }

  public void initWaitFor() {
    initWaitFor = true;
    if (waitFor==null) waitFor = Lists.mutable.empty();
  }
}
