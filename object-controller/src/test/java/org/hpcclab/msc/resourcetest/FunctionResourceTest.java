package org.hpcclab.msc.resourcetest;

import io.quarkus.test.junit.QuarkusTest;
import org.hpcclab.msc.object.entity.MscFuncMetadata;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

@QuarkusTest
public class FunctionResourceTest {
  @Inject
  MscFuncRepository funcRepo;

  @Test
  void find() {
    var map = funcRepo.listByMeta(
      List.of(new MscFuncMetadata().setName("buildin.logical.copy"))
    ).await().indefinitely();
    Assertions.assertEquals(1, map.size());
  }
}
