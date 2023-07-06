package org.hpcclab.oaas;

import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;
import org.hpcclab.oaas.storage.S3ConnConf;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PresignGeneratorTest {
  @Test
  void testPresign() {
    var presigner = S3ClientBuilderUtil.createPresigner(new S3ConnConf() {
      @Override
      public String accessKey() {
        return "aaaaaaaaa";
      }

      @Override
      public String secretKey() {
        return "bbbbbbbbbbb";
      }

      @Override
      public URI url() {
        return URI.create("http://localhost:9000");
      }
      @Override
      public URI publicUrl() {
        return URI.create("http://localhost:9000");
      }

      @Override
      public boolean pathStyle() {
        return true;
      }

      @Override
      public String bucket() {
        return "oprc";
      }

      @Override
      public Optional<String> prefix() {
        return Optional.of("");
      }

      @Override
      public String region() {
        return "us-east-1";
      }
    });
    PresignGenerator presignGenerator = new PresignGenerator(presigner);
    var url = presignGenerator.generatePresignGet("bkt", "obj");
    assertDoesNotThrow(() -> URI.create(url));
  }

}
