package org.hpcclab.msc.stream;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class StreamCreator {
  private static final Logger LOGGER = LoggerFactory.getLogger( StreamCreator.class );
  @Inject
  WebClient client;

  void onStart(@Observes StartupEvent startupEvent) throws ExecutionException, InterruptedException {
    var streams = executeStatement("list streams;")
      .getJsonObject(0).getJsonArray("streams");
    var streamNameList = streams.stream()
      .map(o -> (JsonObject) o)
      .map(o -> o.getString("name")).toList();
    if (!streamNameList.contains("resourceRequests")) {
      executeStatement("""
        CREATE STREAM resourceRequests
        (
          ownerObjectId VARCHAR,
          requestFile VARCHAR
        )
        WITH (kafka_topic='msc-resource-requests', value_format='json');
        """);
    }
    if (!streamNameList.contains("tasks")) {
      executeStatement("""
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

    if (!streamNameList.contains("taskCompletions")) {
      executeStatement("""
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
  }

  JsonArray executeStatement(String sql) {
    var res = client.post("/ksql")
      .sendJsonObject(new JsonObject().put("ksql", sql))
      .await().indefinitely();
    if (res.statusCode() != 200)  {
      LOGGER.error("status is not 200\n===BODY===\n {}", res.bodyAsString());
      throw new RuntimeException("status is not 200");
    }
    return res.bodyAsJsonArray();
  }
}
