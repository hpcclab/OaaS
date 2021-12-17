package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasWorkflowStep implements Serializable {
  String funcName;
  String target;
  String as;
  List<String> inputRefs;

  public OaasWorkflowStep() {
  }

  @ProtoFactory
  public OaasWorkflowStep(String funcName, String target, String as, List<String> inputRefs) {
    this.funcName = funcName;
    this.target = target;
    this.as = as;
    this.inputRefs = inputRefs;
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
}
