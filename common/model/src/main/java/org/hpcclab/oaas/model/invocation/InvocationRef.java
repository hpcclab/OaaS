package org.hpcclab.oaas.model.invocation;

import lombok.Data;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Objects;

@Data
public final class InvocationRef {
  @ProtoField(1)
  final String key;
  @ProtoField(2)
  final String cls;

  @ProtoFactory
  public InvocationRef(String key, String cls) {
    this.key = key;
    this.cls = cls;
  }
}
