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
  @ProtoField(value = 2, defaultValue = "-1")
  long updatedOffset = -1;
  @ProtoField(3)
  String lastInv;

  public ObjectStatus() {
  }


  @ProtoFactory
  public ObjectStatus(long updatedOffset,
                      String lastInv) {
    this.updatedOffset = updatedOffset;
    this.lastInv = lastInv;
  }

  public ObjectStatus copy() {
    return new ObjectStatus(
      updatedOffset,
      lastInv
    );
  }

  public void set(TaskCompletion taskCompletion) {
    if (taskCompletion.isSuccess()) {
      lastInv = taskCompletion.getId().iid();
    }
  }
}
