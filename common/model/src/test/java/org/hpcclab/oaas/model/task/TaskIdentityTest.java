package org.hpcclab.oaas.model.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskIdentityTest {

  @Test
  void test() {
//    System.out.println(Arrays.toString("aa::".split(":", -1)));
    var i = TaskIdentity.decode("aa::");
    assertEquals("aa", i.mid);
    assertNull(i.oid);
    assertNull(i.vid);
    assertEquals("aa::", i.toString());
    i = TaskIdentity.decode("aa:bb:");
    assertEquals("aa", i.mid);
    assertEquals("bb", i.oid);
    assertNull(i.vid);
    assertEquals("aa:bb:", i.toString());
    i = TaskIdentity.decode("aa:bb:cc");
    assertEquals("aa", i.mid);
    assertEquals("bb", i.oid);
    assertEquals("cc", i.vid);
    assertEquals("aa:bb:cc", i.toString());
  }
}
