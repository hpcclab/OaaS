package org.hpcclab.oaas.model.object;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum ObjectAccessModifier {
  @ProtoEnumValue(value = 1, name = "OBJ_PUBLIC")
  PUBLIC,
  @ProtoEnumValue(value = 2, name = "OBJ_INTERNAL")
  INTERNAL,
  @ProtoEnumValue(value = 3, name = "OBJ_FINAL")
  FINAL
}
