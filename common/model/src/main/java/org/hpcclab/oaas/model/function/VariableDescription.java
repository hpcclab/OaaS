package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariableDescription {
  @ProtoField(1)
  String name;

  public VariableDescription() {
  }

  @ProtoFactory
  public VariableDescription(String name) {
    this.name = name;
  }
}
