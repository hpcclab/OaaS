package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionBinding {
  @ProtoField(1)
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;
  @ProtoField(2)
  String function;
  @ProtoField(3)
  String alias;

  public OaasFunctionBinding() {
  }

  @ProtoFactory
  public OaasFunctionBinding(FunctionAccessModifier access,
                             String function,
                             String alias) {
    this.access = access;
    this.function = function;
    this.alias = alias;
  }
}
