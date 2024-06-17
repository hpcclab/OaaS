package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.repository.store.DatastoreConf;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

public class S3ClientBuilderUtil {

  public static S3Presigner createPresigner(DatastoreConf datastoreConf,
                                            boolean usePub) {
    AwsCredentials credentials = AwsBasicCredentials.create(
      datastoreConf.user(),
      datastoreConf.pass()
    );
    var options = datastoreConf.options();
    var pathStyle = Boolean.valueOf(options.getOrDefault("PATHSTYLE", "true"));
    return S3Presigner.builder()
      .endpointOverride(URI.create(usePub ? options.get("PUBLICURL"):options.get("URL")))
      .region(Region.of(options.getOrDefault("REGION", "us-east-1")))
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .serviceConfiguration(S3Configuration.builder()
        .pathStyleAccessEnabled(pathStyle)
        .checksumValidationEnabled(!pathStyle)
        .build())
      .build();
  }
}
