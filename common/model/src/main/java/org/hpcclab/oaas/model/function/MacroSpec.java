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
public class MacroSpec {
  List<DataflowStep> steps;
  Set<DataflowExport> exports;
  String export;
  boolean atomic =false;

  public MacroSpec() {
  }


  public MacroSpec(List<DataflowStep> steps,
                   Set<DataflowExport> exports,
                   String export,
                   boolean atomic) {
    this.steps = steps;
    this.exports = exports;
    this.export = export;
    this.atomic = atomic;
  }

}
