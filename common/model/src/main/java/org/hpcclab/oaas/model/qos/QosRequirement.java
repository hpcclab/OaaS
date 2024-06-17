package org.hpcclab.oaas.model.qos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * @author Pawissanutt
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record QosRequirement(int latency, int throughput, double availability) {
}
