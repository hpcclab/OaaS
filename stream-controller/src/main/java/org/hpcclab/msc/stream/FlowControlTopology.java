package org.hpcclab.msc.stream;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.Stores;
import org.hpcclab.oaas.model.TaskEvent;
import org.hpcclab.oaas.model.TaskState;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class FlowControlTopology {

  @Inject
  FlowControlConfig config;

  @Produces
  public Topology buildTopology() {
    final StreamsBuilder builder = new StreamsBuilder();
    var tsSerde = new ObjectMapperSerde<>(
      TaskState.class);
    var teSerde = new ObjectMapperSerde<>(
      TaskEvent.class);

    final String storeName = config.stateStoreName();
    final String teTopic = config.taskEventTopic();

    var tsStoreBuilder = Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(storeName),
      Serdes.String(),
      tsSerde
    );

    builder
      .addStateStore(tsStoreBuilder)
      .stream(teTopic, Consumed.with(Serdes.String(), teSerde))
      .flatTransform(
        () -> new TaskEventTransformer(storeName),
        storeName
      )
      .to(teTopic, Produced.with(Serdes.String(), teSerde));

    return builder.build();
  }
}
