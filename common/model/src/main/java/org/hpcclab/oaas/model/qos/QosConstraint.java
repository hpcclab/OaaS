package org.hpcclab.oaas.model.qos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;

/**
 * @author Pawissanutt
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QosConstraint {
  @ProtoField(1)
  int budget;
  @ProtoField(2)
  ConsistencyModel consistency;
  @ProtoField(3)
  String geographical;
  @ProtoField(4)
  boolean persistent;
  @ProtoField(5)
  List<String> runtimeRequirements;

  public QosConstraint() {
  }

  @ProtoFactory
  public QosConstraint(int budget,
                       ConsistencyModel consistency,
                       String geographical,
                       boolean persistent,
                       List<String> runtimeRequirements) {
    this.budget = budget;
    this.consistency = consistency;
    this.geographical = geographical;
    this.persistent = persistent;
    this.runtimeRequirements = runtimeRequirements;
  }
}
