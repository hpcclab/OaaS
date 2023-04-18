package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.testcontainers.arangodb.containers.ArangoContainer;

import java.util.Map;

public class ArangoResource implements
  QuarkusTestResourceLifecycleManager {
  private static final ArangoContainer container = new ArangoContainer("3.10").withoutAuth();

  @Override
  public Map<String, String> start() {
    container.start();
    return Map.of(
      "oaas.repo.arg.port", String.valueOf(container.getPort()),
      "oaas.ispn.argConnection.port", String.valueOf(container.getPort())
//      "oaas.ispn.argConnection.port", "8529",
//      "oaas.ispn.argConnection.pass", "changeme"
    );
  }

  @Override
  public void stop() {
    container.stop();
  }
}
