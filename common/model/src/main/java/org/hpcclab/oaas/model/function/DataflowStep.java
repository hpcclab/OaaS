package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataflowStep implements Serializable {
  String function;
  String target;
  String as;
  List<String> inputRefs;
  DSMap args;
  DSMap argRefs;

  public DataflowStep() {
  }

  @ProtoFactory
  public DataflowStep(String function, String target, String as, List<String> inputRefs, DSMap args, DSMap argRefs) {
    this.function = function;
    this.target = target;
    this.as = as;
    this.inputRefs = inputRefs;
    this.args = args;
    this.argRefs = argRefs;
  }


  @ProtoField(1)
  public String getFunction() {
    return function;
  }

  @ProtoField(2)
  public String getTarget() {
    return target;
  }

  @ProtoField(3)
  public String getAs() {
    return as;
  }

  @ProtoField(4)
  public List<String> getInputRefs() {
    return inputRefs;
  }


  @ProtoField(5)
  public DSMap getArgs() {
    return args;
  }

  @ProtoField(6)
  public DSMap getArgRefs() {
    return argRefs;
  }
}
