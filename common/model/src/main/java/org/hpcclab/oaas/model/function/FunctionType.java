package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum FunctionType {
  @ProtoEnumValue(1)
  TASK,
  @ProtoEnumValue(2)
  MACRO,
  @ProtoEnumValue(3)
  LOGICAL,
  @ProtoEnumValue(4)
  READONLY,
  @ProtoEnumValue(5)
  STATIC,
  @ProtoEnumValue(6)
  STATIC_READONLY,
}
