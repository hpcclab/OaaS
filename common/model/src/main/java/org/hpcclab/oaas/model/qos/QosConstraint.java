package org.hpcclab.oaas.model.qos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * @author Pawissanutt
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record QosConstraint(int budget,
                            ConsistencyModel consistency,
                            String geographical,
                            boolean ephemeral) {
}
