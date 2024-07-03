package org.hpcclab.oaas.model.qos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * @author Pawissanutt
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record QosRequirement(int throughput,
                             Locality locality,
                             double availability) {

  public enum Locality {
    HOST, CLUSTER, REGION, NONE
  }
}
