package org.hpcclab.oaas.model.proto;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

import java.util.UUID;

@ProtoAdapter(UUID.class)
public final class UUIDAdapter {
  public UUIDAdapter() {
  }

  @ProtoFactory
  UUID create(Long mostSigBitsFixed, Long leastSigBitsFixed) {
    return new UUID(mostSigBitsFixed, leastSigBitsFixed);
  }

  @ProtoField(
    number = 3,
    type = Type.FIXED64,
    defaultValue = "0"
  )
  Long getMostSigBitsFixed(UUID uuid) {
    return uuid.getMostSignificantBits();
  }

  @ProtoField(
    number = 4,
    type = Type.FIXED64,
    defaultValue = "0"
  )
  Long getLeastSigBitsFixed(UUID uuid) {
    return uuid.getLeastSignificantBits();
  }
}
