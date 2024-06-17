package org.hpcclab.oaas;

import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PresignGeneratorTest {
  @Test
  void testPresign() {
    var conf = DatastoreConf.builder()
      .user("aaaa")
      .pass("bbbb")
      .options(Map.of("URL", "http://localhost:9000"))
      .build();
    var presigner = S3ClientBuilderUtil.createPresigner(conf, false);

    PresignGenerator presignGenerator = new PresignGenerator(presigner);
    var url = presignGenerator.generatePresignGet("bkt", "obj");
    assertDoesNotThrow(() -> URI.create(url));
  }

}
