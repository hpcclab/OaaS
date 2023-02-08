package org.hpcclab.oaas.model.task;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TaskIdentityTest {

  @Test
  void test() {
//    System.out.println(Arrays.toString("aa::".split(":", -1)));
    var i = TaskIdentity.decode("aa::");
    assertEquals("aa", i.mId);
    assertNull(i.oId);
    assertNull(i.vId);
    assertEquals("aa::", i.toString());
    i = TaskIdentity.decode("aa:bb:");
    assertEquals("aa", i.mId);
    assertEquals("bb", i.oId);
    assertNull(i.vId);
    assertEquals("aa:bb:", i.toString());
    i = TaskIdentity.decode("aa:bb:cc");
    assertEquals("aa", i.mId);
    assertEquals("bb", i.oId);
    assertEquals("cc", i.vId);
    assertEquals("aa:bb:cc", i.toString());
  }
}
