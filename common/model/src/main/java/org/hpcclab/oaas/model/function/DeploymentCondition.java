package org.hpcclab.oaas.model.function;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum DeploymentCondition {
  @ProtoEnumValue(1)
  PENDING,
  @ProtoEnumValue(2)
  DEPLOYING,
  @ProtoEnumValue(3)
  RUNNING,
  @ProtoEnumValue(4)
  DOWN,
  @ProtoEnumValue(5)
  DELETED
}
