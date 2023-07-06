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
public class MacroSpec implements Serializable {
  @ProtoField(1)
  List<DataflowStep> steps;
  @ProtoField(2)
  Set<WorkflowExport> exports;
  @ProtoField(3)
  String export;
  @ProtoField(value = 4,defaultValue = "false")
  boolean atomic =false;

  public MacroSpec() {
  }

  @ProtoFactory
  public MacroSpec(List<DataflowStep> steps,
                   Set<WorkflowExport> exports,
                   String export,
                   boolean atomic) {
    this.steps = steps;
    this.exports = exports;
    this.export = export;
    this.atomic = atomic;
  }

}
