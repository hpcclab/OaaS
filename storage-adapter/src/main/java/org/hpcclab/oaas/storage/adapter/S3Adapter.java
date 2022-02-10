package org.hpcclab.oaas.storage.adapter;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.storage.DataAccessRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

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
  public Uni<Response> loadPutUrls(DataAccessRequest dar, List<String> keys) {
    if (keys== null || keys.isEmpty()) {
      keys = dar.getCls().getStateSpec().getKeySpecs()
        .stream()
        .map(KeySpecification::getName)
        .toList();
    }
    return Multi.createFrom().iterable(keys)
      .map(key -> Tuple2.of(key, generatePresigned(Method.PUT, dar.getOid() + "/" + key)))
      .collect().asList()
      .map(list -> {
        var jo = new JsonObject();
        for (var t : list) {
          jo.put(t.getItem1(), t.getItem2());
        }
        return Response.ok(jo).build();
      });
  }

  @Override
  public Uni<Response> get(DataAccessRequest dar) {
    return Uni.createFrom()
      .item(() -> generatePresigned(Method.GET, dar.getOid() + "/" + dar.getKey()))
      .map(url -> Response.temporaryRedirect(URI.create(url)).build());
  }

  private String generatePresigned(Method method, String path) {
    try {
      var args  = GetPresignedObjectUrlArgs.builder()
        .method(method)
        .bucket(bucket)
        .object(path)
        .build();
      return minioClient.getPresignedObjectUrl(args);
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
}
