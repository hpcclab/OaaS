package org.hpcclab.oaas.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;

@ApplicationScoped
public class S3Adapter implements StorageAdapter{
  @Inject
  MinioClient minioClient;
  @ConfigProperty(
    name = "oaas.sa.s3.bucket",
    defaultValue = "oaas-bkt"
  )
  String bucket;

  @Override
  public Uni<Response> get(DataAccessRequest crc) {
    try {
      var args  = GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(bucket)
        .object(crc.getOid() + "/" + crc.getKey())
        .build();
      var url = minioClient.getPresignedObjectUrl(args);
      return Uni.createFrom()
        .item(Response.temporaryRedirect(URI.create(url)).build());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Uni<Response> put(DataAccessRequest crc) {
    return null;
  }

  @Override
  public Uni<Response> delete(DataAccessRequest crc) {
    return null;
  }
}
