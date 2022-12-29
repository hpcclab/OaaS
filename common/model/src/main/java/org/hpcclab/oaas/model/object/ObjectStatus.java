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
  long crtTs;
  @ProtoField(value = 3, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long queTs;
  @ProtoField(value = 4, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long smtTs;
  @ProtoField(value = 5, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long cptTs;
  @ProtoField(6)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  List<String> waitFor = List.of();
  @ProtoField(value = 7, defaultValue = "false")
  @JsonIgnore
  boolean initWaitFor = false;
  @ProtoField(8)
  @JsonIgnore
  String originator;
  @ProtoField(9)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  String errorMsg;

  public ObjectStatus() {
  }


  @ProtoFactory
  public ObjectStatus(TaskStatus taskStatus,
                      long crtTs,
                      long queTs,
                      long smtTs,
                      long cptTs,
                      List<String> waitFor,
                      boolean initWaitFor,
                      String originator,
                      String errorMsg) {
    this.taskStatus = taskStatus;
    this.crtTs = crtTs;
    this.queTs = queTs;
    this.smtTs = smtTs;
    this.cptTs = cptTs;
    this.waitFor = waitFor;
    this.initWaitFor = initWaitFor;
    this.originator = originator;
    this.errorMsg = errorMsg;
  }

  public ObjectStatus copy() {
    return new ObjectStatus(
      taskStatus,
      crtTs,
      queTs,
      smtTs,
      cptTs,
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
    if (taskCompletion.getCptTs() > 0 ) {
      cptTs = taskCompletion.getCptTs();
    } else {
      cptTs = System.currentTimeMillis();
    }
    if (taskCompletion.getSmtTs() > 0) {
      smtTs = taskCompletion.getSmtTs();
    }
    errorMsg = taskCompletion.getErrorMsg();
    var ext = taskCompletion.getExt();
    if (ext!=null && ext.containsKey("osts")) {
      try {
        smtTs = Long.parseLong(ext.get("osts"));
      } catch (NumberFormatException ignore) {
      }
    }
  }

  public void initWaitFor() {
    initWaitFor = true;
    if (waitFor==null) waitFor = Lists.mutable.empty();
  }
}
