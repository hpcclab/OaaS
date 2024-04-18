package org.hpcclab.oaas.model.object;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum OObjectType {
  @ProtoEnumValue(1)
  SIMPLE,
  @ProtoEnumValue(2)
  COMPOUND,
}
