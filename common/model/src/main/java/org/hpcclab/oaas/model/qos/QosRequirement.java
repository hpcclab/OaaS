package org.hpcclab.oaas.model.qos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author Pawissanutt
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QosRequirement {
  @ProtoField(1)
  int latency;
  @ProtoField(2)
  int throughput;
  @ProtoField(3)
  float availability;

  public QosRequirement() {
  }

  @ProtoFactory
  public QosRequirement(int latency, int throughput, float availability) {
    this.latency = latency;
    this.throughput = throughput;
    this.availability = availability;
  }
}
