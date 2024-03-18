package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.test.MockClassControllerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class DataflowFnControllerTest {
  MockClassControllerRegistry classControllerRegistry;

  @BeforeEach
  void beforeEach() {
    classControllerRegistry = MockClassControllerRegistry.mock();
  }

  @Test
  public void test() {
    assertThat(classControllerRegistry.classControllerMap.size())
      .isNotZero();
    System.out.println(classControllerRegistry.printStructure());
  }
}
