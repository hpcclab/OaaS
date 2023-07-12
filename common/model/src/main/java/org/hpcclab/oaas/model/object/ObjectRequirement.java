package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectRequirement implements Serializable{
  @ProtoField(2)
  String cls;

  public ObjectRequirement() {
  }

  @ProtoFactory
  public ObjectRequirement(String cls) {
    this.cls = cls;
  }
}
