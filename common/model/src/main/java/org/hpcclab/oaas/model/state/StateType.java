package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum StateType {
  @ProtoEnumValue(1)
  FILES,
  @ProtoEnumValue(2)
  COLLECTION,
}
