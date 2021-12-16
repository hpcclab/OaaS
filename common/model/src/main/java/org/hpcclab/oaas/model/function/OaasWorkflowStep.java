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
  @ProtoField(1)
  String funcName;
  @ProtoField(2)
  String target;
  @ProtoField(3)
  String as;
  @ProtoField(4)
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
}
