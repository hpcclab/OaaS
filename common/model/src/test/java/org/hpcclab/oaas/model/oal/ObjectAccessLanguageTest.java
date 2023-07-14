package org.hpcclab.oaas.model.oal;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ObjectAccessLanguageTest {

  String id() {
    return UUID.randomUUID().toString();
  }

  @Test
  void testValid() {
    assertTrue(ObjectAccessLanguage.validate(id()));
    assertTrue(ObjectAccessLanguage.validate("%s:test".formatted(id())));
    assertTrue(ObjectAccessLanguage.validate("%s:test()".formatted(id())));
    assertTrue(ObjectAccessLanguage.validate("%s:test(%s)".formatted(id(),id())));
    assertTrue(ObjectAccessLanguage.validate("%s:test(%s,%s)"
      .formatted(id(),id(), id())));
    assertTrue(ObjectAccessLanguage.validate("%s:test(%s,%s,%s)"
      .formatted(id(),id(), id(), id())));
    assertTrue(ObjectAccessLanguage.validate(
      "%s:test()()".formatted(id())));
    assertTrue(ObjectAccessLanguage.validate(
      "%s:test()(test=aaa)".formatted(id())));
    assertTrue(ObjectAccessLanguage.validate(
      "%s:test()(aaa=111,bbb=222)".formatted(id())));
    assertTrue(ObjectAccessLanguage.validate(
      "%s:test(%s)(aaa=111,bbb=222)".formatted(id(),id())));
    assertTrue(ObjectAccessLanguage.validate(
      "%s:test(%s,%s)(aaa=111,bbb=222)".formatted(id(),id(),id())));
  }
  @Test
  void testInvalid() {
    assertFalse(ObjectAccessLanguage.validate(id()+':'));
    assertFalse(ObjectAccessLanguage.validate("%s:test(TE__)".formatted(id())));
    assertFalse(ObjectAccessLanguage.validate("%s:test()())".formatted(id())));
  }

  @Test
  void testParse() {
    var ids = List.of(id(),id(),id(),id());
    var fc = ObjectAccessLanguage.parse(
      ids.get(0)
    );
    assertNotNull(fc);
    assertEquals(ids.get(0), fc.getMain());
    assertNull(fc.getFb());

    fc = ObjectAccessLanguage.parse(
      "%s:test".formatted(ids.get(0))
    );
    assertNotNull(fc);
    assertEquals(ids.get(0), fc.getMain());
    assertEquals("test", fc.fb);
    assertNull(fc.getInputs());
    assertNull(fc.getArgs());

    fc = ObjectAccessLanguage.parse(
      "%s:test()".formatted(ids.get(0))
    );
    assertNotNull(fc);
    assertEquals(ids.get(0), fc.getMain());
    assertEquals("test", fc.fb);
    assertNull(fc.getInputs());
    assertNull(fc.getArgs());


    fc = ObjectAccessLanguage.parse(
      "%s:test(%s)".formatted(ids.get(0), ids.get(1))
    );
    assertNotNull(fc);
    assertEquals(ids.get(0), fc.getMain());
    assertEquals("test", fc.fb);
    assertNotNull(fc.getInputs());
    assertEquals(1, fc.getInputs().size());
    assertEquals(ids.get(1), fc.getInputs().get(0));
    assertNull(fc.getArgs());

    fc = ObjectAccessLanguage.parse(
      "%s:test(%s,%s)()".formatted(ids.get(0), ids.get(1), ids.get(2))
    );
    assertNotNull(fc);
    assertEquals(ids.get(0), fc.getMain());
    assertEquals("test", fc.fb);
    assertNotNull(fc.getInputs());
    assertEquals(2, fc.getInputs().size());
    assertEquals(ids.get(1), fc.getInputs().get(0));
    assertEquals(ids.get(2), fc.getInputs().get(1));
    assertNull(fc.getArgs());


    fc = ObjectAccessLanguage.parse(
      "%s:test(%s,%s,%s)(aaa=bbb)".formatted(ids.get(0), ids.get(1), ids.get(2),
        ids.get(3))
    );
    assertNotNull(fc);
    assertEquals(ids.get(0), fc.getMain());
    assertEquals("test", fc.fb);
    assertNotNull(fc.getInputs());
    assertEquals(3, fc.getInputs().size());
    assertEquals(ids.get(1), fc.getInputs().get(0));
    assertEquals(ids.get(2), fc.getInputs().get(1));
    assertEquals(ids.get(3), fc.getInputs().get(2));
    assertNotNull(fc.getArgs());
    assertEquals(1, fc.getArgs().size());
    assertEquals("bbb", fc.getArgs().get("aaa"));
    assertNull(fc.getArgs().get("ccc"));

    fc = ObjectAccessLanguage.parse(
      "%s:test(%s,%s,%s)(aaa=111,122-/*=*/-++})".formatted(ids.get(0), ids.get(1), ids.get(2),
        ids.get(3))
    );
    assertNotNull(fc);
    assertEquals(ids.get(0),fc.getMain());
    assertEquals("test",fc.fb);
    assertNotNull(fc.getInputs());
    assertEquals(3, fc.getInputs().size());
    assertEquals(ids.get(1), fc.getInputs().get(0));
    assertEquals(ids.get(2), fc.getInputs().get(1));
    assertEquals(ids.get(3), fc.getInputs().get(2));
    assertNotNull(fc.getArgs());
    assertEquals(2, fc.getArgs().size());
    assertEquals("111", fc.getArgs().get("aaa"));
    assertEquals("*/-++}", fc.getArgs().get("122-/*"));
  }

  @Test
  void testToString() {
    var ids = IntStream.range(0,3)
      .mapToObj(i -> UUID.randomUUID().toString())
      .toList();
    var fc = ObjectAccessLanguage.builder()
      .main(ids.get(0))
      .build();
    assertEquals(
      ids.get(0),
      fc.toString()
    );

    fc = ObjectAccessLanguage.builder()
      .main(ids.get(0))
      .fb("test")
      .build();
    assertEquals(
      ids.get(0) + ":test()",
      fc.toString()
    );

    fc = ObjectAccessLanguage.builder()
      .cls("testCls")
      .fb("test")
      .build();
    assertEquals(
      "_testCls:test()",
      fc.toString()
    );

    fc = ObjectAccessLanguage.builder()
      .main(ids.get(0))
      .fb("test")
      .inputs(List.of(ids.get(1)))
      .build();
    assertEquals(
      "%s:test(%s)".formatted(ids.get(0),ids.get(1)),
      fc.toString()
    );

    fc = ObjectAccessLanguage.builder()
      .main(ids.get(0))
      .fb("more.test")
      .inputs(List.of(ids.get(1),ids.get(2)))
      .build();
    assertEquals(
      "%s:more.test(%s,%s)".formatted(ids.get(0),ids.get(1), ids.get(2)),
      fc.toString()
    );

    fc = ObjectAccessLanguage.builder()
      .main(ids.get(0))
      .fb("more.test")
      .inputs(List.of(ids.get(1),ids.get(2)))
      .args(Map.of("aaa","bbb"))
      .build();
    assertEquals(
      "%s:more.test(%s,%s)(aaa=bbb)".formatted(ids.get(0),ids.get(1), ids.get(2)),
      fc.toString()
    );

    fc = ObjectAccessLanguage.builder()
      .main(ids.get(0))
      .fb("more.test")
      .inputs(List.of(ids.get(1),ids.get(2)))
      .args(Map.of("aaa","bbb", "231aa^()", "-*/++"))
      .build();

    assertEquals(
      "%s:more.test(%s,%s)(231aa^()=-*/++,aaa=bbb)".formatted(ids.get(0),ids.get(1), ids.get(2)),
      fc.toString()
    );
  }
}
