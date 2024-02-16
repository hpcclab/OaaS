package org.hpcclab.oaas.controller.model;

import io.restassured.internal.common.assertion.Assertion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pawissanutt
 */
class CrHashTest {

  List<CrHash.ApiAddress> gen(int i){
    return IntStream.range(0,i).mapToObj(j -> new CrHash.ApiAddress("aaa", 11)).toList();
  }
  @Test
  void merge() {
    var h1 = new CrHash("aa", 10, gen(10));
    var h2 = new CrHash("aa", 5, gen(5));
    var h3 = CrHash.merge(h1,h2);
    assertThat(h3.numSegment()).isEqualTo(10);
    h1 = new CrHash("aa", 10, gen(10));
    h2 = new CrHash("aa", 10, gen(10));
    h3 = CrHash.merge(h1,h2);
    assertThat(h3.numSegment()).isEqualTo(10);
    h1 = new CrHash("aa", 10, gen(10));
    h2 = new CrHash("aa", 20, gen(20));
    h3 = CrHash.merge(h1,h2);
    assertThat(h3.numSegment()).isEqualTo(20);
  }
}
