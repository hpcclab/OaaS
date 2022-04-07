package org.hpcclab.oaas.model.object;

import org.infinispan.protostream.annotations.ProtoEnumValue;

import java.util.stream.Stream;

public enum OaasObjectType {
  @ProtoEnumValue(1)
  SIMPLE,
  @ProtoEnumValue(2)
  COMPOUND,
  @ProtoEnumValue(3)
  STREAM
}
