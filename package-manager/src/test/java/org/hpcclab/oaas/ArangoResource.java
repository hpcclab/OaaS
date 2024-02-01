package org.hpcclab.oaas;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.testcontainers.arangodb.containers.ArangoContainer;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Map;

public class ArangoResource implements
  QuarkusTestResourceLifecycleManager {
  private static final ArangoContainer container = new ArangoContainer("arangodb:3.11").withoutAuth();

  @Override
  public Map<String, String> start() {
    var env = ConfigProvider.getConfig().getValue("oprc.env", String.class);
    container.start();
    env = env.replaceAll("PORT=8529\\n", "PORT="+container.getPort() + "\n");
    return Map.of(
      "oprc.env", env
    );
  }

  @Override
  public void stop() {
    container.stop();
  }
}
