package org.hpcclab.oaas.model.object;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum OaasObjectType {
  @ProtoEnumValue(0)
  SIMPLE,
  @ProtoEnumValue(1)
  COMPOUND
}
