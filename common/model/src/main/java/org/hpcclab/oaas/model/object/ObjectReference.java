package org.hpcclab.oaas.model.object;

import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class ObjectReference {
  String name;
  String object;

  public ObjectReference() {
  }

  @ProtoFactory
  public ObjectReference(String name, String object) {
    this.name = name;
    this.object = object;
  }

  @ProtoField(1)
  public String getName() {
    return name;
  }

  @ProtoField(2)
  public String getObject() {
    return object;
  }
}
