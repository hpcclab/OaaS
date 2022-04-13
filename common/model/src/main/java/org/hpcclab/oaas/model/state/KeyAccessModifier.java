package org.hpcclab.oaas.model.state;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum KeyAccessModifier {
  @ProtoEnumValue(value = 1, name = "KEY_PUBLIC")
  PUBLIC,
  @ProtoEnumValue(value = 2, name = "KEY_INTERNAL")
  INTERNAL,
  @ProtoEnumValue(value = 3, name = "KEY_PRIVATE")
  PRIVATE
}
