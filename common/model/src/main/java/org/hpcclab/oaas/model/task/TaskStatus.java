package org.hpcclab.oaas.model.task;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum TaskStatus {
  @ProtoEnumValue(1)
  SUCCEEDED,
  @ProtoEnumValue(2)
  FAILED
}
