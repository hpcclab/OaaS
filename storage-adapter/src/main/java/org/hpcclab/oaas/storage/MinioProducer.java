package org.hpcclab.oaas.storage;

import io.minio.MinioClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class MinioProducer {
  @ConfigProperty(name = "oaas.sa.s3.accessKey")
  String accessKey;
  @ConfigProperty(name = "oaas.sa.s3.secretKey")
  String secretKey;
  @ConfigProperty(name = "oaas.sa.s3.url")
  String url;
  @ConfigProperty(name = "oaas.sa.s3.region", defaultValue = "us-east-1")
  String region;

  @Produces
  public MinioClient minioClient() {
    return MinioClient.builder()
      .endpoint(url)
      .region(region)
      .credentials(accessKey, secretKey)
      .build();
  }
}
