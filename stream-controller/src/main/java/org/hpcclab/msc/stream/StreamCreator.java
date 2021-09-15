package org.hpcclab.msc.stream;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class StreamCreator {
  private static final Logger LOGGER = LoggerFactory.getLogger( StreamCreator.class );
  @Inject
  KsqlService ksqlService;

  void onStart(@Observes StartupEvent startupEvent) {
    var streams = ksqlService.executeStatement("list streams;")
      .getJsonObject(0).getJsonArray("streams");
    var streamNameList = streams.stream()
      .map(o -> (JsonObject) o)
      .map(o -> o.getString("name")).toList();
    if (!streamNameList.contains("RESOURCEREQUESTS")) {
      ksqlService.executeStatement("""
        CREATE STREAM resourceRequests
        (
          ownerObjectId VARCHAR,
          requestFile VARCHAR
        )
        WITH (kafka_topic='msc-resource-requests', value_format='json');
        """);
    }

    if (!streamNameList.contains("TASKS")) {
      ksqlService.executeStatement("""
        CREATE STREAM tasks
        (
          mainObj string,
          outputObj string,
          functionName string,
          image string,
          commands ARRAY<string>,
          containerArgs ARRAY<string>,
          env MAP<string,string>
        )
        WITH (kafka_topic='msc-tasks', value_format='json');
        """);
    }

    if (!streamNameList.contains("TASKCOMPLETIONS")) {
      ksqlService.executeStatement("""
        CREATE STREAM taskCompletions
        (
          mainObj string,
          outputObj string,
          functionName string,
          status string,
          startTime string,
          completionTime string,
          debugMessage string
        )
        WITH (kafka_topic='msc-task-completions', value_format='json');
        """);
    }
    createTable();
  }

  void createTable() {
    var tables = ksqlService.executeStatement("list tables;")
      .getJsonObject(0).getJsonArray("tables");
    var tableNames = tables.stream()
      .map(o -> (JsonObject) o)
      .map(o -> o.getString("name")).toList();
  }
}
