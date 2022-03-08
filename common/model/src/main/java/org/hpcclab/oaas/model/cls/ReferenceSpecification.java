package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferenceSpecification {
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String cls;

  public ReferenceSpecification() {
  }

  @ProtoFactory
  public ReferenceSpecification(String name, String cls) {
    this.name = name;
    this.cls = cls;
  }
}
