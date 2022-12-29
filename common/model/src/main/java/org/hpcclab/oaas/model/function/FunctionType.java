package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum FunctionType {
  /**
   * The type for mutable task
   */
  @ProtoEnumValue(1) TASK,
  /**
   * The type for immutable task
   */
  @ProtoEnumValue(2) IM_TASK,
  @ProtoEnumValue(3) LOGICAL,
  @ProtoEnumValue(4) MACRO,
  @ProtoEnumValue(5) STATIC,
  @ProtoEnumValue(6) READONLY,
  @ProtoEnumValue(7) STATIC_READONLY,
}
