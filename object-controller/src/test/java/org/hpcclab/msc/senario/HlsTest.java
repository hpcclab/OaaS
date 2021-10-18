package org.hpcclab.msc.senario;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hpcclab.msc.TestUtils;
import org.hpcclab.msc.object.entity.object.OaasCompoundMember;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.state.OaasObjectState;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class HlsTest {

  @BeforeAll
  static void setup() {
    RestAssured.filters(new RequestLoggingFilter(),
      new ResponseLoggingFilter());
  }

  @Test
  void test() {
    var m3u8Obj = new OaasObject()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setState(new OaasObjectState().setBaseUrl("http://test/test.m3u8"));
    var segmentsObj = new OaasObject()
      .setType(OaasObject.ObjectType.RESOURCE)
      .setState(new OaasObjectState()
        .setBaseUrl("http://test/segment")
      )
//      .setFunctions(Set.of("builtin.hls.ts.transcode"))
      ;

    m3u8Obj = TestUtils.create(m3u8Obj);
    segmentsObj = TestUtils.create(segmentsObj);
    var hlsObject = new OaasObject()
      .setType(OaasObject.ObjectType.COMPOUND)
      .setMembers(
        List.of(
          new OaasCompoundMember("m3u8", m3u8Obj),
          new OaasCompoundMember("segments", segmentsObj)
      ));

    hlsObject = TestUtils.create(hlsObject);
    hlsObject = TestUtils.bind(hlsObject, List.of("builtin.hls.macro.transcode"));
    var hls2 = TestUtils.fnCall(new FunctionCallRequest().setFunctionName("builtin.hls.macro.transcode")
      .setTarget(hlsObject.getId()));
    hls2.getMembers().stream()
      .map(OaasCompoundMember::getObject)
      .map(OaasObject::getId)
      .forEach(TestUtils::getObject);
  }

}
