package org.hpcclab.oaas.model.object;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum ObjectType {
  @ProtoEnumValue(1)
  SIMPLE,
  @ProtoEnumValue(2)
  COMPOUND,
  @ProtoEnumValue(3)
  STREAM
}
