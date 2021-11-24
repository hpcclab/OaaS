package org.hpcclab.oaas.taskgen.stream;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.hpcclab.oaas.model.task.BaseTaskMessage;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.TaskState;
import org.hpcclab.oaas.taskgen.TaskEventManager;
import org.hpcclab.oaas.taskgen.TaskManagerConfig;
import org.hpcclab.oaas.taskgen.deserializer.TaskEventDeserializer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class FlowControlTopology {

  @Inject
  TaskManagerConfig config;
  @Inject
  TaskEventManager taskEventManager;

  @Produces
  public Topology buildTopology() {
    final StreamsBuilder builder = new StreamsBuilder();
    var tsSerde = new ObjectMapperSerde<>(
      TaskState.class);

//    Serde<TaskEvent> taskEventSerde = new ObjectMapperSerde<>(TaskEvent.class);


    final String storeName = config.stateStoreName();
    final String teTopic = config.taskEventTopic();

    var tsStoreBuilder = Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(storeName),
      Serdes.String(),
      tsSerde
    );

//    builder
//      .addStateStore(tsStoreBuilder);
//      .stream(teTopic, Consumed.with(Serdes.String(), taskEventSerde))
//      .flatTransform(
//        () -> new TaskEventTransformer(storeName,taskEventManager),
//        storeName
//      )
//      .split()
//      .branch((key, value) -> value instanceof OaasTask,
//          Branched.withConsumer(ks -> ks.to(config.taskTopic(),  Produced.with(Serdes.String(),
//            new ObjectMapperSerde(OaasTask.class))))
//        )
//      .defaultBranch(Branched.withConsumer(ks -> ks.to(teTopic, Produced.with(Serdes.String(),
//        new ObjectMapperSerde(TaskEvent.class)))));

    return builder.build()
      .addSource(
        "source",
        Serdes.String().deserializer(),
        new TaskEventDeserializer(),
        teTopic
      )
      .addProcessor(
        "flow-control",
        new ProcessorSupplier<String, TaskEvent, String, BaseTaskMessage>() {
          @Override
          public Set<StoreBuilder<?>> stores() {
            return Set.of(tsStoreBuilder);
          }

          @Override
          public Processor<String, TaskEvent, String, BaseTaskMessage> get() {
            return new FlowControlProcessor(storeName,
              taskEventManager,
              config.enableCloudEventHeaders());
          }
        },
        "source"
      )
      .addSink(
        "tasks",
        config.taskTopic(),
        Serdes.String().serializer(),
        new ObjectMapperSerializer<>(),
        "flow-control"
      )
      .addSink(
        "task-events",
        config.taskEventTopic(),
        Serdes.String().serializer(),
        new ObjectMapperSerializer<>(),
        "flow-control"
      );
  }
}
