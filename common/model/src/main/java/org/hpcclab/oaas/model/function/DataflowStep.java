package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataflowStep implements Serializable {
  String funcName;
  String target;
  String as;
  List<String> inputRefs;
  Map<String,String> args;
  Map<String,String> argRefs;

  public DataflowStep() {
  }

  public DataflowStep(String funcName, String target, String as, List<String> inputRefs, Map<String, String> args, Map<String, String> argRefs) {
    this.funcName = funcName;
    this.target = target;
    this.as = as;
    this.inputRefs = inputRefs;
    this.args = args;
    this.argRefs = argRefs;
  }

  @ProtoFactory
  public DataflowStep(String funcName, String target, String as, List<String> inputRefs, HashMap<String, String> args, HashMap<String, String> argRefs) {
    this.funcName = funcName;
    this.target = target;
    this.as = as;
    this.inputRefs = inputRefs;
    this.args = args;
    this.argRefs = argRefs;
  }



  @ProtoField(1)
  public String getFuncName() {
    return funcName;
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


  @ProtoField(number = 5, javaType = HashMap.class)
  public Map<String, String> getArgs() {
    return args;
  }

  @ProtoField(number = 6, javaType = HashMap.class)
  public Map<String, String> getArgRefs() {
    return argRefs;
  }
}
