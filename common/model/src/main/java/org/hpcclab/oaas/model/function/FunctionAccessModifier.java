package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum FunctionAccessModifier {
  @ProtoEnumValue(1)
  PUBLIC,
  @ProtoEnumValue(2)
  INTERNAL,
  @ProtoEnumValue(3)
  PRIVATE
}
