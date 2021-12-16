package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum OaasFunctionType {
  @ProtoEnumValue(1)
  TASK,
  @ProtoEnumValue(2)
  MACRO,
  @ProtoEnumValue(3)
  LOGICAL
}
