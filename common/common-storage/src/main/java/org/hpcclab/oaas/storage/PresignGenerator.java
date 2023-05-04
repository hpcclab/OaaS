package org.hpcclab.oaas.storage;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;


public class PresignGenerator {
  S3Presigner s3Presigner;

  public PresignGenerator(S3Presigner s3Presigner) {
    this.s3Presigner = s3Presigner;
  }

  public String generatePresignGet(String bkt, String path) {
    return s3Presigner.presignGetObject(r -> r
        .getObjectRequest(get -> get
          .bucket(bkt)
          .key(path)
          .build())
        .signatureDuration(Duration.ofMinutes(15))
        .build())
      .url().toString();
  }
  public String generatePresignPut(String bkt, String path) {
    return s3Presigner.presignPutObject(r -> r
        .putObjectRequest(get -> get
          .bucket(bkt)
          .key(path)
          .build())
        .signatureDuration(Duration.ofMinutes(15))
        .build())
      .url().toString();
  }
}
