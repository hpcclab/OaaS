package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum FunctionType {
  /**
   * The type for mutable task
   */
  TASK(true),
  /**
   * The type for immutable task
   */
  IM_TASK(false),
  LOGICAL(true),
  MACRO(false),
  CHAIN(false),
//  STATIC(false),
//  READONLY(false),
//  STATIC_READONLY(false)
  ;
  final boolean mutable;

  FunctionType(boolean allowUpdateMain) {
    this.mutable = allowUpdateMain;
  }

  public boolean isMutable() {
    return mutable;
  }
}
