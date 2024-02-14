package org.hpcclab.oaas.model.state;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum KeyAccessModifier {
  /**
   * Allow all
   */
  @ProtoEnumValue(value = 1, name = "KEY_PUBLIC")
  PUBLIC,
  /**
   * Allow any function if it was invoked as the dependency(e.g., input, reference)
   */
  @ProtoEnumValue(value = 2, name = "KEY_DEPENDENT")
  DEPENDENT,
  /**
   * Allow only function in the same package
   */
  @ProtoEnumValue(value = 3, name = "KEY_INTERNAL")
  INTERNAL,
  /**
   * Allow only function in the same class
   */
  @ProtoEnumValue(value = 4, name = "KEY_PRIVATE")
  PRIVATE
}
