package org.hpcclab.oaas.provisioner.provisioner;

import io.smallrye.reactive.messaging.kafka.Record;

import java.util.function.Consumer;

public interface Provisioner<T> {
  Consumer<T> provision(Record<String, T> functionRecord);
}
