package org.hpcclab.oaas.pm.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Pawissanutt
 */
class CrHashTest {

  List<CrHash.ApiAddress> gen(int i){
    return IntStream.range(0,i)
      .mapToObj(j -> new CrHash.ApiAddress("aaa", 11, System.currentTimeMillis())).toList();
  }
  @Test
  void merge() {
    var h1 = new CrHash("aa", 10, gen(10), System.currentTimeMillis());
    var h2 = new CrHash("aa", 5, gen(5), System.currentTimeMillis());
    var h3 = CrHash.merge(h1,h2);
    assertThat(h3.numSegment()).isEqualTo(5);
    h1 = new CrHash("aa", 10, gen(10), System.currentTimeMillis());
    h2 = new CrHash("aa", 10, gen(10), System.currentTimeMillis());
    h3 = CrHash.merge(h1,h2);
    assertThat(h3.numSegment()).isEqualTo(10);
    h1 = new CrHash("aa", 10, gen(10), System.currentTimeMillis());
    h2 = new CrHash("aa", 20, gen(20), System.currentTimeMillis());
    h3 = CrHash.merge(h1,h2);
    assertThat(h3.numSegment()).isEqualTo(20);
  }
}
