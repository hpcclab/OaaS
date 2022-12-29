package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum FunctionAccessModifier {
  /**
   * Allow all
   */
  @ProtoEnumValue(1)
  PUBLIC,
  /**
   * Allow only macro function in the same package
   */
  @ProtoEnumValue(2)
  INTERNAL,
  /**
   * Allow only macro function in the same class
   */
  @ProtoEnumValue(3)
  PRIVATE
}
