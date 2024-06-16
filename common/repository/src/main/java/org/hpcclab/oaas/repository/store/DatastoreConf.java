package org.hpcclab.oaas.repository.store;


import lombok.Builder;

import java.util.Map;

@Builder(toBuilder = true)
public record DatastoreConf(String name,
                            String type,
                            String host,
                            int port,
                            String user,
                            String pass,
                            Map<String, String> options) {
}
