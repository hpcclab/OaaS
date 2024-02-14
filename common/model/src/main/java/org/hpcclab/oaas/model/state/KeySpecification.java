package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeySpecification {
  @ProtoField(1)
  String name;
  @ProtoField(2)
  KeyAccessModifier access = KeyAccessModifier.PUBLIC;

  public KeySpecification() {
  }

  public KeySpecification(String name) {
    this.name = name;
  }


  @ProtoFactory
  public KeySpecification(String name,  KeyAccessModifier access) {
    this.name = name;
    this.access = access;
  }
}
