package org.hpcclab.oaas.invocation.config;

import lombok.Builder;
import lombok.Getter;

@Builder()
public record InvocationConfig(String storageAdapterUrl) {
}
