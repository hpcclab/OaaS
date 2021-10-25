package org.hpcclab.msc.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.Json;
import org.hpcclab.msc.TestUtils;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@QuarkusTest
public class FunctionResourceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger( FunctionResourceTest.class );
  @Inject
  OaasFuncRepository funcRepo;

  public static final String DUMMY_FUNCTION = """
    - name: test.dummy.resource
      type: TASK
      outputCls: builtin.basic.file
      validation: {}
      task: {}
    - name: test.dummy.compound
      type: MACRO
      outputCls: builtin.basic.compound
      validation: {}
      macro:
          steps:
            - funcName: builtin.logical.copy
              target: obj1
              as: new_obj1
              inputRefs: []
            - funcName: builtin.logical.copy
              target: obj2
              as: new_obj2
              inputRefs: []
          exports:
            - from: new_obj1
              as: obj1
            - from: new_obj2
              as: obj2
    """;

  @Test
  void find() {
    var functions = funcRepo.listByNames(
      List.of("builtin.logical.copy")
    ).await().indefinitely();
    Assertions.assertEquals(1, functions.size());
  }


  @Test
  void create() {
    var functionList = TestUtils.createFunctionYaml(DUMMY_FUNCTION);
//    LOGGER.debug("functionList {}", Json.encodePrettily(functionList));
  }
}
