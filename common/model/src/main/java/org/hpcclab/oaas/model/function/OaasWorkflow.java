package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasWorkflow implements Serializable {
  @ProtoField(1)
  List<OaasWorkflowStep> steps;
  @ProtoField(2)
  Set<OaasWorkflowExport> exports;
  @ProtoField(3)
  String export;

  public OaasWorkflow() {
  }

  @ProtoFactory
  public OaasWorkflow(List<OaasWorkflowStep> steps,
                      Set<OaasWorkflowExport> exports,
                      String export) {
    this.steps = steps;
    this.exports = exports;
    this.export = export;
  }

}
