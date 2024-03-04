package org.hpcclab.oaas.crm.optimize;

import lombok.Builder;

/**
 * @author Pawissanutt
 */
@Builder(toBuilder = true)
public record CrDataSpec(int replication) {
}
