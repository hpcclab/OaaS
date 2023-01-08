package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum FunctionType {
  /**
   * The type for mutable task
   */
  @ProtoEnumValue(1) TASK(true),
  /**
   * The type for immutable task
   */
  @ProtoEnumValue(2) IM_TASK(false),
  @ProtoEnumValue(3) LOGICAL(true),
  @ProtoEnumValue(4) MACRO(false),
  @ProtoEnumValue(5) STATIC(false),
  @ProtoEnumValue(6) READONLY(false),
  @ProtoEnumValue(7) STATIC_READONLY(false);
  boolean allowUpdateMain;

  FunctionType(boolean allowUpdateMain) {
    this.allowUpdateMain = allowUpdateMain;
  }

  public boolean isAllowUpdateMain() {
    return allowUpdateMain;
  }
}
