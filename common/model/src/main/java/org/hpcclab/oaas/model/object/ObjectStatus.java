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
  long createdTs;
  @ProtoField(value = 3, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long submittedTs;
  @ProtoField(value = 4, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long completedTs;
  @ProtoField(5)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  List<String> waitFor = List.of();
  @ProtoField(value = 6, defaultValue = "false")
  @JsonIgnore
  boolean initWaitFor = false;
  @ProtoField(7)
  @JsonIgnore
  String originator;
  @ProtoField(8)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  String errorMsg;

  public ObjectStatus() {
  }


  @ProtoFactory
  public ObjectStatus(TaskStatus taskStatus, long createdTs, long submittedTs, long completedTs, List<String> waitFor, boolean initWaitFor, String originator,
                      String errorMsg) {
    this.taskStatus = taskStatus;
    this.createdTs = createdTs;
    this.submittedTs = submittedTs;
    this.completedTs = completedTs;
    this.waitFor = waitFor;
    this.initWaitFor = initWaitFor;
    this.originator = originator;
    this.errorMsg = errorMsg;
  }

  public ObjectStatus copy() {
    return new ObjectStatus(
      taskStatus,
      createdTs,
      submittedTs,
      completedTs,
      waitFor==null ? null:List.copyOf(waitFor),
      initWaitFor,
      originator,
      errorMsg
    );
  }

  public void set(TaskCompletion taskCompletion) {
    if (taskCompletion.isSuccess())
      taskStatus = TaskStatus.SUCCEEDED;
    else
      taskStatus = TaskStatus.FAILED;
    if (taskCompletion.getTs() > 0 ) {
      completedTs = taskCompletion.getTs();
    } else {
      completedTs = System.currentTimeMillis();
    }
    errorMsg = taskCompletion.getErrorMsg();
    var ext = taskCompletion.getExtensions();
    if (ext!=null && ext.containsKey("osts")) {
      try {
        submittedTs = Long.parseLong(ext.get("osts"));
      } catch (NumberFormatException ignore) {
      }
    }
  }

  public void initWaitFor() {
    initWaitFor = true;
    if (waitFor==null) waitFor = Lists.mutable.empty();
  }
}
