package org.hpcclab.msc.stream;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.hpcclab.oaas.model.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.net.HostName;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

@ApplicationScoped
public class TaskStateQuery {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskStateQuery.class );
  @Inject
  KafkaStreams streams;
  @Inject
  FlowControlConfig config;

  HttpClient client;

  @PostConstruct
  void setup() {
    client = HttpClient.newBuilder()
      .build();
  }

  public TaskState getTaskState(String key) {
    KeyQueryMetadata metadata = streams.queryMetadataForKey(
      config.stateStoreName(),
      key,
      Serdes.String().serializer());

    if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
      LOGGER.warn("Found no metadata for key {}", key);
      return null;
    } else if (metadata.activeHost().host().equals(HostName.getQualifiedHostName())) {
      LOGGER.info("Found data for key {} locally", key);
      TaskState result = getTaskStateStore().get(key);

      if (result != null) {
        return result;
      } else {
        return null;
      }
    } else {
      LOGGER.info("Found data for key {} on remote host {}:{}",
        key, metadata.activeHost().host(), metadata.activeHost().port());
      // TODO
      HttpRequest httpRequest = HttpRequest.newBuilder()
        .build();
      return null;
    }
  }

  private ReadOnlyKeyValueStore<String, TaskState> getTaskStateStore() {
    while (true) {
      try {
        return streams.store(StoreQueryParameters.fromNameAndType(config.stateStoreName(),
          QueryableStoreTypes.keyValueStore()));
      } catch (InvalidStateStoreException e) {
        // ignore, store not ready yet
      }
    }
  }
}
