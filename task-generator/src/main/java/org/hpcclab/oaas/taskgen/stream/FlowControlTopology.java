package org.hpcclab.oaas.taskgen.stream;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.Stores;
import org.hpcclab.oaas.model.task.BaseTaskMessage;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.taskgen.TaskEventManager;
import org.hpcclab.oaas.taskgen.TaskGeneratorConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class FlowControlTopology {

  @Inject
  TaskGeneratorConfig config;
  @Inject
  TaskEventManager taskEventManager;

  @Produces
  public Topology buildTopology() {
    final StreamsBuilder builder = new StreamsBuilder();
    var tsSerde = new ObjectMapperSerde<>(
      TaskState.class);

    Serde<TaskEvent> taskEventSerde = new ObjectMapperSerde<>(TaskEvent.class);


    final String storeName = config.stateStoreName();
    final String teTopic = config.taskEventTopic();

    var tsStoreBuilder = Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(storeName),
      Serdes.String(),
      tsSerde
    );

    builder
      .addStateStore(tsStoreBuilder)
      .stream(teTopic, Consumed.with(Serdes.String(), taskEventSerde))
      .flatTransform(
        () -> new TaskEventTransformer(storeName,taskEventManager),
        storeName
      )
      .split()
      .branch((key, value) -> value instanceof OaasTask,
          Branched.withConsumer(ks -> ks.to(config.taskTopic(),  Produced.with(Serdes.String(),
            new ObjectMapperSerde(OaasTask.class))))
        )
      .defaultBranch(Branched.withConsumer(ks -> ks.to(teTopic, Produced.with(Serdes.String(),
        new ObjectMapperSerde(TaskEvent.class)))));
    return builder.build();
  }
}
