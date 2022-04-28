package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionBinding {
  @ProtoField(1)
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;
  @ProtoField(2)
  String function;
  @ProtoField(3)
  String name;

  @ProtoField(4)
  Set<String> forwardRecords;

  public OaasFunctionBinding() {
  }

  @ProtoFactory
  public OaasFunctionBinding(FunctionAccessModifier access, String function, String name, Set<String> forwardRecords) {
    this.access = access;
    this.function = function;
    this.name = name;
    this.forwardRecords = forwardRecords;
  }
}
