package org.hpcclab.oaas.crm.template;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoQosRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class CrTemplateManagerTest {
  @Inject
  CrTemplateManager manager;

  @BeforeEach
  void setUp() {
    manager.loadTemplate();
  }

  @Test
  void test() {
    DeploymentUnit deployment = DeploymentUnit.newBuilder()
      .setCls(ProtoOClass.newBuilder()
        .setQos(ProtoQosRequirement.newBuilder().setThroughput(1001))
      )
      .build();
    CrTemplate template = manager.selectTemplate(deployment);
    System.out.println(template.getConfig());
    assertEquals(10, template.getConfig().priority());
  }
}
