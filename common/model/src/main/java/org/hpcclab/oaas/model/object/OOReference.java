package org.hpcclab.oaas.model.object;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.state.KeyAccessModifier;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
public class OOReference {
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String objId;
  @ProtoField(3)
  KeyAccessModifier access;

  public OOReference() {
  }

  @ProtoFactory
  public OOReference(String name, String objId,
                     KeyAccessModifier access) {
    this.name = name;
    this.objId = objId;
    this.access = access;
  }
}
