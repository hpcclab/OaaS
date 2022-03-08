package org.hpcclab.oaas.model.object;

import lombok.Data;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
public class ObjectTaskStatus {

  @ProtoField(value = 1,defaultValue = "-1")
  long completionTime = -1;
  @ProtoField(2)
  TaskStatus status = TaskStatus.WAITING;

  public ObjectTaskStatus() {
  }

  @ProtoFactory
  public ObjectTaskStatus(long completionTime, TaskStatus status) {
    this.completionTime = completionTime;
    this.status = status;
  }
}
