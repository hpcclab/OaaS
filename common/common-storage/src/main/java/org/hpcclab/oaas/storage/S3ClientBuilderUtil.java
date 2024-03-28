package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.repository.store.DatastoreConf;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

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
  public static S3Presigner createPresigner(S3ConnConf connConf,
                                            boolean usePub) {
    AwsCredentials credentials = AwsBasicCredentials.create(
      connConf.accessKey(),
      connConf.secretKey()
    );
    return S3Presigner.builder()
      .endpointOverride(usePub? connConf.publicUrl(): connConf.url())
      .region(Region.of(connConf.region()))
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

  public static S3Presigner createPresigner(DatastoreConf datastoreConf,
                                                           boolean usePub) {
    AwsCredentials credentials = AwsBasicCredentials.create(
      datastoreConf.user(),
      datastoreConf.pass()
    );
    var options = datastoreConf.options();
    var pathStyle = Boolean.valueOf(options.getOrDefault("PATHSTYLE", "true"));
    return S3Presigner.builder()
      .endpointOverride(URI.create(usePub? options.get("PUBLICURL"):options.get("URL")))
      .region(Region.of(options.getOrDefault("REGION","us-east-1")))
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .serviceConfiguration(S3Configuration.builder()
        .pathStyleAccessEnabled(pathStyle)
        .checksumValidationEnabled(!pathStyle)
        .build())
      .build();
  }
}
