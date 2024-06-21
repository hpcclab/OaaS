package org.hpcclab.oaas.repository.store;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Map;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record DatastoreConf(String name,
                            String type,
                            String host,
                            int port,
                            String user,
                            String pass,
                            Map<String, String> options) {
}
