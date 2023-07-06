package org.hpcclab.oaas.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class S3ClientBuilderUtil {
  public static S3Client createClient(S3ConnConf connConf) {
    AwsCredentials credentials = AwsBasicCredentials.create(
      connConf.accessKey(),
      connConf.secretKey()
    );
    return S3Client.builder()
      .region(Region.US_EAST_1)
      .endpointOverride(connConf.url())
      .forcePathStyle(connConf.pathStyle())
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .build();
  }
  public static S3Presigner createPresigner(S3ConnConf connConf) {
    AwsCredentials credentials = AwsBasicCredentials.create(
      connConf.accessKey(),
      connConf.secretKey()
    );
    return S3Presigner.builder()
      .endpointOverride(connConf.url())
      .region(Region.US_EAST_1)
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .serviceConfiguration(S3Configuration.builder()
        .pathStyleAccessEnabled(connConf.pathStyle())
        .checksumValidationEnabled(!connConf.pathStyle())
        .build())
      .build();
  }
  public static S3Presigner.Builder createPresignerBuilder(S3ConnConf connConf) {
    AwsCredentials credentials = AwsBasicCredentials.create(
      connConf.accessKey(),
      connConf.secretKey()
    );
    return S3Presigner.builder()
      .endpointOverride(connConf.url())
      .region(Region.US_EAST_1)
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .serviceConfiguration(S3Configuration.builder()
        .pathStyleAccessEnabled(connConf.pathStyle())
        .checksumValidationEnabled(!connConf.pathStyle())
        .build());
  }
}
