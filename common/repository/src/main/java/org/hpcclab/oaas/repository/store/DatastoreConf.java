package org.hpcclab.oaas.repository.store;


import java.util.Map;

public record DatastoreConf(String name, String type, String host, int port, String user, String pass,
                            Map<String, String> options) {
}
