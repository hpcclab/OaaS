package org.hpcclab.msc.stream;


import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;
import org.hpcclab.msc.object.model.ObjectResourceRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.time.Duration;

@ApplicationScoped
public class DeduplicatedTopology {


  final String storeName = "dedup-store";
  final String inputTopic = "request";
  final String outputTopic = "distinct-request";

  @Produces
  public Topology buildTopology() {
    final StreamsBuilder builder = new StreamsBuilder();
    ObjectMapperSerde<ObjectResourceRequest> orrSerde = new ObjectMapperSerde<>(
      ObjectResourceRequest.class);

    // How long we "remember" an event.  During this time, any incoming duplicates of the event
    // will be, well, dropped, thereby de-duplicating the input data.
    //
    // The actual value depends on your use case.  To reduce memory and disk usage, you could
    // decrease the size to purge old windows more frequently at the cost of potentially missing out
    // on de-duplicating late-arriving records.
    final Duration windowSize = Duration.ofDays(7);
    final Duration retentionPeriod = windowSize;

    final StoreBuilder<WindowStore<String, Long>> dedupStoreBuilder = Stores.windowStoreBuilder(
      Stores.persistentWindowStore(storeName,
        retentionPeriod,
        windowSize,
        false
      ),
      Serdes.String(),
      Serdes.Long()
    );

    builder.addStateStore(dedupStoreBuilder);

    builder
      .stream(inputTopic, Consumed.with(Serdes.String(), orrSerde))
      .transformValues(() -> new DeduplicationTransformer<>(
        windowSize.toMillis(),
        (key, value) -> value.getOwnerObjectId() + "/" + value.getRequestFile(),
        storeName),
        storeName
      )
      .filter((k, v) -> v!=null)
      .to(outputTopic, Produced.with(Serdes.String(), orrSerde));


    return builder.build();
  }
}
