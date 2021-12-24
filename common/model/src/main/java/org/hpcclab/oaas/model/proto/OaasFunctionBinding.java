package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionBinding {
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;
  String function;

  public OaasFunctionBinding() {
  }

  @ProtoFactory
  public OaasFunctionBinding(FunctionAccessModifier access, String function) {
    this.access = access;
    this.function = function;
  }

  @ProtoField(1)
  public FunctionAccessModifier getAccess() {
    return access;
  }

  @ProtoField(2)
  public String getFunction() {
    return function;
  }
}
