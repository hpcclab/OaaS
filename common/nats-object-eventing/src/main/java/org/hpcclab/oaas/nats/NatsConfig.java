package org.hpcclab.oaas.nats;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "oaas.nats")
public interface NatsConfig {

  Optional<String> natsUrls();

}
