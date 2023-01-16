package org.hpcclab.oaas.invoker;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionDeploymentStatus;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@QuarkusTest
@QuarkusTestResource(ArangoResource.class)
public class DeployerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger( DeployerTest.class );

  @Inject
  FunctionRepository funcRepo;
  @Inject
  VerticleDeployer deployer;

  OaasFunction function;

  @BeforeEach
  void setup() {
    function = new OaasFunction();
    function.setPkg("test")
      .setName("fn")
      .setDeploymentStatus(new FunctionDeploymentStatus()
        .setCondition(DeploymentCondition.RUNNING)
        .setInvocationUrl("http://localhost:8080")
      )
      .setType(FunctionType.TASK);
    funcRepo.persistAsync(function).await().indefinitely();
  }

  @Test
  void test() {
    deployer.deployVerticleIfNew(function.getKey())
      .await().indefinitely();
    LOGGER.info("verticleIds {}", deployer.getVerticleIds());
    Assertions.assertTrue(deployer.getVerticleIds().containsKey(function.getKey()));
    deployer.deleteVerticle(function.getKey())
      .await().indefinitely();
    LOGGER.info("verticleIds {}", deployer.getVerticleIds());
    Assertions.assertFalse(deployer.getVerticleIds().containsKey(function.getKey()));
  }
}
