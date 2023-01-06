package org.hpcclab.oaas.storage.adapter;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.model.data.DataAccessRequest;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.storage.SaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class S3Adapter implements StorageAdapter {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3Adapter.class);
  @Inject
  SaConfig config;
  @Inject
  Vertx vertx;

  private MinioClient minioClient;
  private MinioClient publicMinioClient;
  private boolean relay;
  private WebClient webClient;
  private String prefix;

  void setup(@Observes StartupEvent event) {
    var s3Config = config.s3();
    minioClient = MinioClient.builder()
      .endpoint(s3Config.url())
      .region(s3Config.region())
      .credentials(s3Config.accessKey(), s3Config.secretKey())
      .build();
    publicMinioClient = MinioClient.builder()
      .endpoint(s3Config.publicUrl())
      .region(s3Config.region())
      .credentials(s3Config.accessKey(), s3Config.secretKey())
      .build();
    relay = config.s3().relay();
    webClient = WebClient.create(vertx);
    prefix = s3Config.prefix()==null ? "":s3Config.prefix();
  }

  @Override
  public String name() {
    return "s3";
  }

  @Override
  public Uni<Response> get(DataAccessRequest dar) {
    var uni = Uni.createFrom()
      .item(() -> generatePresigned(
        Method.GET,
        convertToPath(dar.oid(), dar.vid(), dar.key()),
        false)
      );
    if (relay) {
      return uni
        .flatMap(this::relay);
    } else {
      return uni
        .map(url -> Response.temporaryRedirect(URI.create(url)).build());
    }
  }

  public Uni<Response> relay(String url) {
    return webClient.getAbs(url)
      .send()
      .map(resp -> {
        if (resp.statusCode()==200) {
          var buffer = resp.bodyAsBuffer();
          LOGGER.debug("Relaying data from '{}' with {} bytes",
            url, buffer==null ? 0:buffer.length());
          return Response.ok(buffer).build();
        } else {
          LOGGER.warn("Error relaying data from '{}' code {}",
            url, resp.statusCode());
          return Response.status(Response.Status.BAD_GATEWAY)
            .build();
        }
      });
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
      throw new StdOaasException(e.getMessage(), e);
    }
  }

  @Override
  public Uni<Map<String, String>> allocate(InternalDataAllocateRequest request) {
    return Uni.createFrom().item(allocateBlocking(request));
  }

  public Map<String, String> allocateBlocking(InternalDataAllocateRequest request) {
    var keys = request.keys();
    var map = new HashMap<String, String>();
    for (String key : keys) {
      var path = generatePath(request, key);
      var url = generatePresigned(
        Method.PUT,
        path,
        request.publicUrl());
      map.put(key, url);
    }
    return map;
  }

  String generatePath(InternalDataAllocateRequest request, String key) {
    return convertToPath(request.oid(), request.vid(), key);
  }

  String convertToPath(String oid, String vid, String key) {
    return prefix +
      oid +
      '/' +
      vid +
      '/' +
      key;
  }
}
