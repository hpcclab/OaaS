package org.hpcclab.oaas.storage.adapter;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hpcclab.oaas.model.data.DataAccessRequest;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.storage.SaConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class S3Adapter implements StorageAdapter {
  private MinioClient minioClient;
  private MinioClient publicMinioClient;
  @Inject
  SaConfig config;

  void setup(@Observes StartupEvent event) {
    var s3Config = config.s3();
    minioClient =  MinioClient.builder()
      .endpoint(s3Config.url())
      .region(s3Config.region())
      .credentials(s3Config.accessKey(), s3Config.secretKey())
      .build();
    publicMinioClient = MinioClient.builder()
      .endpoint(s3Config.publicUrl())
      .region(s3Config.region())
      .credentials(s3Config.accessKey(), s3Config.secretKey())
      .build();
  }

  @Override
  public String name() {
    return "s3";
  }

  @Override
  public Uni<Response> get(DataAccessRequest dar) {
    return Uni.createFrom()
      .item(() -> generatePresigned(
        Method.GET,
        dar.getOid() + "/" + dar.getKey(),
        false)
      )
      .map(url -> Response.temporaryRedirect(URI.create(url)).build());
  }

  private String generatePresigned(Method method,
                                   String path,
                                   boolean isPublicUrl) {
    try {
      var client = isPublicUrl ? publicMinioClient:minioClient;
      var args = GetPresignedObjectUrlArgs.builder()
        .method(method)
        .bucket(config.s3().bucket())
        .object(path)
        .build();
      return client.getPresignedObjectUrl(args);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Uni<Response> put(DataAccessRequest dar) {
    return null;
  }

  @Override
  public Uni<Response> delete(DataAccessRequest dar) {
    return null;
  }

  @Override
  public Uni<Map<String, String>> allocate(DataAllocateRequest request) {
    var keys = request.getKeys().get(name());
    var map = new HashMap<String, String>();
    for (String key : keys) {
      var url = generatePresigned(Method.PUT,
        request.getOid() + "/" + key,
        true);
      map.put(key, url);
    }
    return Uni.createFrom().item(map);
  }
}
