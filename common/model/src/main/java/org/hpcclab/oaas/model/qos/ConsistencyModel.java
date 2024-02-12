package org.hpcclab.oaas.model.qos;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * @author Pawissanutt
 */
public enum ConsistencyModel {
  @ProtoEnumValue(1)
  NONE,
  @ProtoEnumValue(2)
  EVENTUAL,
  @ProtoEnumValue(3)
  SEQUENTIAL,
  @ProtoEnumValue(4)
  LINEARIZATION
}
