package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum FunctionState {
  @ProtoEnumValue(1)
  ENABLED,
  @ProtoEnumValue(2)
  DISABLED,
  @ProtoEnumValue(3)
  REMOVING
}
