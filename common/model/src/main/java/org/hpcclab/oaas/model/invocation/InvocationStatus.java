package org.hpcclab.oaas.model.invocation;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum InvocationStatus {
  @ProtoEnumValue(1)
  LAZY(false,false, false),
  @ProtoEnumValue(2)
  DOING(true,false, false),
  @ProtoEnumValue(3)
  SUCCEEDED(true,true, false),
  @ProtoEnumValue(4)
  FAILED(true,true, true),
  @ProtoEnumValue(5)
  DEPENDENCY_FAILED(false,false, true),
  @ProtoEnumValue(6)
  READY(false,true, false);

  final boolean submitted;
  final boolean completed;
  final boolean failed;

  InvocationStatus(boolean submitted, boolean completed, boolean failed) {
    this.submitted = submitted;
    this.completed = completed;
    this.failed = failed;
  }

  public boolean isSubmitted() {
    return submitted;
  }

  public boolean isCompleted() {
    return completed;
  }

  public boolean isFailed() {
    return failed;
  }
}
