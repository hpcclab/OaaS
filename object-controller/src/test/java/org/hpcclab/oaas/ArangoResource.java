package org.hpcclab.oaas;

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
      "oaas.repo.arg.port", String.valueOf(container.getPort())
    );
  }

  @Override
  public void stop() {
    container.stop();
  }
}
