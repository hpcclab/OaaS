package org.hpcclab.oaas.model.object;

import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
public class ObjectReference {
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String objId;

  public ObjectReference() {
  }

  @ProtoFactory
  public ObjectReference(String name, String objId) {
    this.name = name;
    this.objId = objId;
  }
}
