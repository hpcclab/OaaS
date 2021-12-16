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
  private List<OaasWorkflowStep> steps;
  private Set<OaasWorkflowExport> exports;

  public OaasWorkflow() {
  }

  @ProtoFactory
  public OaasWorkflow(List<OaasWorkflowStep> steps, Set<OaasWorkflowExport> exports) {
    this.steps = steps;
    this.exports = exports;
  }

  @ProtoField(1)
  public List<OaasWorkflowStep> getSteps() {
    return steps;
  }

  @ProtoField(2)
  public Set<OaasWorkflowExport> getExports() {
    return exports;
  }
}
