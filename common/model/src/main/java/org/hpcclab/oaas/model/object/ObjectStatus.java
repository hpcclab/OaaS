package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ObjectStatus implements Copyable<ObjectStatus> {
  @ProtoField(1)
  TaskStatus taskStatus = TaskStatus.LAZY;
  @ProtoField(value = 2, defaultValue = "-1")
  long updatedOffset = -1;
  @ProtoField(3)
  String vid;

  public ObjectStatus() {
  }


  @ProtoFactory
  public ObjectStatus(TaskStatus taskStatus,
                      long updatedOffset,
                      String vid) {
    this.taskStatus = taskStatus;
    this.updatedOffset = updatedOffset;
    this.vid = vid;
  }

  public ObjectStatus copy() {
    return new ObjectStatus(
      taskStatus,
      updatedOffset,
      vid
    );
  }

  public void set(TaskCompletion taskCompletion) {
    if (taskCompletion.isSuccess()) {
      taskStatus = TaskStatus.SUCCEEDED;
      vid = taskCompletion.getId().getVid();
    } else
      taskStatus = TaskStatus.FAILED;
  }
}
