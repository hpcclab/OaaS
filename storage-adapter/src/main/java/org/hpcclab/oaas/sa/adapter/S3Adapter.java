package org.hpcclab.oaas.sa.adapter;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.hpcclab.oaas.model.data.DataAccessRequest;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.hpcclab.oaas.sa.SaConfig;
import org.hpcclab.oaas.storage.PresignGeneratorPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class S3Adapter implements StorageAdapter {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3Adapter.class);
  PresignGeneratorPool generatorPool;
  private final boolean relay;
  private final WebClient webClient;
  private final String prefix;
  private final String bkt;

  @Inject
  public S3Adapter(SaConfig config, Vertx vertx, PresignGeneratorPool generatorPool) {
    this.generatorPool = generatorPool;
    var datastoreConf = DatastoreConfRegistry.getDefault()
      .getOrDefault("S3DEFAULT");
    bkt = datastoreConf.options().get("BUCKET");
    prefix = datastoreConf.options().get("PREFIXPATH");

    relay = config.relay();
    webClient = WebClient.create(vertx);
  }

  @Override
  public String name() {
    return "s3";
  }

  @Override
  public Uni<Response> get(DataAccessRequest dar) {
    var generator = generatorPool.getGenerator();
    var uni = Uni.createFrom()
      .item(() -> generator.generatePresignGet(
          bkt,
          convertToPath(dar.oid(), dar.vid(), dar.key())
        )
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

  @Override
  public Uni<Map<String, String>> allocate(InternalDataAllocateRequest request) {
    return Uni.createFrom().item(allocateBlocking(request));
  }

  public Map<String, String> allocateBlocking(InternalDataAllocateRequest request) {
    var keys = request.keys();
    var map = new HashMap<String, String>();
    for (String key : keys) {
      var path = generatePath(request, key);
      var gen = request.publicUrl() ?
        generatorPool.getPublicGenerator():
        generatorPool.getGenerator();
      var url = gen.generatePresignPut(bkt, path);
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
