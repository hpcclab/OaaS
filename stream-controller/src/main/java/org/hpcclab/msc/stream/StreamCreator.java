package org.hpcclab.msc.stream;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class StreamCreator {
  private static final Logger LOGGER = LoggerFactory.getLogger( StreamCreator.class );
  @Inject
  KsqlService ksqlService;

  public void create() {
    var streams = ksqlService.executeStatement("list streams;")
      .getJsonObject(0).getJsonArray("streams");
    var streamNames = streams.stream()
      .map(o -> (JsonObject) o)
      .map(o -> o.getString("name")).toList();
    var tables = ksqlService.executeStatement("list tables;")
      .getJsonObject(0).getJsonArray("tables");
    var tableNames = tables.stream()
      .map(o -> (JsonObject) o)
      .map(o -> o.getString("name")).toList();

    if (!streamNames.contains("RESOURCEREQUESTS")) {
      ksqlService.executeStatement("""
        CREATE STREAM resourceRequests
        (
          ownerObjectId VARCHAR,
          requestFile VARCHAR
        )
        WITH (kafka_topic='msc-resource-requests', FORMAT='json');
        """);
      LOGGER.info("CREATE STREAM resourceRequests");
    }

    if (!streamNames.contains("TASKS")) {
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
        WITH (kafka_topic='msc-tasks', FORMAT='json');
        """);
      LOGGER.info("CREATE STREAM tasks");
    }

    if (!streamNames.contains("TASKCOMPLETIONS")) {
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
        WITH (kafka_topic='msc-task-completions', FORMAT='json');
        """);
      LOGGER.info("CREATE STREAM taskCompletions");
    }

    if (!tableNames.contains("DETECTED_RESOURCEREQUESTS")) {
      ksqlService.executeStatement("""
        CREATE TABLE DETECTED_RESOURCEREQUESTS AS
        SELECT
            ownerObjectId AS KEY1,
            requestFile AS KEY2,
            AS_VALUE(ownerObjectId) AS ownerObjectId,
            AS_VALUE(requestFile) AS requestFile
        FROM RESOURCEREQUESTS WINDOW TUMBLING (SIZE 1 HOURS, RETENTION 1000 DAYS)
        GROUP BY ownerObjectId, requestFile
        HAVING COUNT(ownerObjectId) = 1;
        """,
        Map.of("auto.offset.reset", "earliest"));
      LOGGER.info("CREATE TABLE DETECTED_RESOURCEREQUESTS");
    }

    if (!streamNames.contains("RAW_DISTINCT_RESOURCEREQUESTS")) {
      ksqlService.executeStatement("""
        CREATE STREAM RAW_DISTINCT_RESOURCEREQUESTS
        (
          ownerObjectId string,
          requestFile string
        )
        WITH (KAFKA_TOPIC = 'DETECTED_RESOURCEREQUESTS',
              FORMAT = 'JSON');
        """,
        Map.of("auto.offset.reset", "earliest"));
      LOGGER.info("CREATE STREAM RAW_DISTINCT_RESOURCEREQUESTS");
    }

    if (!streamNames.contains("DISTINCT_RESOURCEREQUESTS")) {
      ksqlService.executeStatement("""
          CREATE STREAM DISTINCT_RESOURCEREQUESTS AS
          SELECT
              ownerObjectId,
              requestFile
          FROM RAW_DISTINCT_RESOURCEREQUESTS
          WHERE ownerObjectId IS NOT NULL;
          """,
        Map.of("auto.offset.reset", "earliest"));
      LOGGER.info("CREATE STREAM DISTINCT_RESOURCEREQUESTS");
    }
  }

}
