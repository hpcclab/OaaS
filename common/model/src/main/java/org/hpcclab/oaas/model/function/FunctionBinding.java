package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionBinding {
  @ProtoField(1)
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;
  @ProtoField(2)
  String function;
  @ProtoField(3)
  String name;
  @ProtoField(4)
  Set<String> forwardRecords;
  @ProtoField(value = 5, javaType = HashMap.class)
  Map<String, String> defaultArgs;
  @ProtoField(6)
  String description;

  public FunctionBinding() {
  }

  public FunctionBinding(FunctionAccessModifier access, String function, String name, Set<String> forwardRecords, Map<String, String> defaultArgs, String description) {
    this.access = access;
    this.function = function;
    this.name = name;
    this.forwardRecords = forwardRecords;
    this.defaultArgs = defaultArgs;
    this.description = description;
  }

  @ProtoFactory
  public FunctionBinding(FunctionAccessModifier access,
                         String function,
                         String name,
                         Set<String> forwardRecords,
                         HashMap<String,String> defaultArgs,
                         String description) {
    this.access = access;
    this.function = function;
    this.name = name;
    this.forwardRecords = forwardRecords;
    this.defaultArgs = defaultArgs;
    this.description = description;
  }
}
