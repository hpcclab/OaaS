package org.hpcclab.oaas.model.task;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum TaskStatus {
  @ProtoEnumValue(1)
  SUCCEEDED(true,true),
  @ProtoEnumValue(2)
  FAILED(true,false),
  @ProtoEnumValue(3)
  DOING(true,false),
  @ProtoEnumValue(4)
  WAITING(false,false),
  @ProtoEnumValue(5)
  DEPENDENCY_FAILED(true,false);

  final boolean submitted;
  final boolean completed;

  TaskStatus(boolean submitted, boolean completed) {
    this.submitted = submitted;
    this.completed = completed;
  }

  public boolean isSubmitted() {
    return submitted;
  }

  public boolean isCompleted() {
    return completed;
  }
}
