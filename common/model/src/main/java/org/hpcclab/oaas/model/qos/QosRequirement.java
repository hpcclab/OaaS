package org.hpcclab.oaas.model.qos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * @author Pawissanutt
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record QosRequirement(int throughput,
//                             int latency,
                             ColdStartMode coldStart,
                             Locality locality,
                             double availability) {
  public enum ColdStartMode {
    ALLOW, NONE
  }

  public enum Locality {
    HOST, CLUSTER, REGION, NONE
  }
}
