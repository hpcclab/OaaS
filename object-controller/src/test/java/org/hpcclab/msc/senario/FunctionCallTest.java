package org.hpcclab.msc.senario;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hpcclab.msc.TestUtils;
import org.hpcclab.msc.object.entity.state.FileState;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class FunctionCallTest {
  @BeforeAll
  static void setup() {
    RestAssured.filters(new RequestLoggingFilter(),
      new ResponseLoggingFilter());
  }

  @Test
  void testCopy() {
    var root = new MscObject()
      .setType(MscObject.Type.RESOURCE)
      .setState(new FileState().setFileUrl("http://test/test.m3u8"));
    root = TestUtils.create(root);

    var newObj = TestUtils.fnCall(
      new FunctionCallRequest().setFunctionName("buildin.logical.copy").setTarget(root.getId()));
  }
}
