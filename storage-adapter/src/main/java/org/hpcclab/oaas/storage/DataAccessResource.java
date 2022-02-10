package org.hpcclab.oaas.storage;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.jboss.resteasy.reactive.RestQuery;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Path("/contents")
public class DataAccessResource {

  @Inject
  OaasClassRepository clsRepo;
  @Inject
  S3Adapter s3Adapter;
  Map<String, StorageAdapter> adapterMap = Map.of();

  @PostConstruct
  void setup() {
    adapterMap = Map.of("s3", s3Adapter);
  }

  @GET
  @Path("{oid}/{key}")
  public Uni<Response> get(String oid,
                        String key,
                        @RestQuery byte[] contextKey) {
    var dacJson = Base64.getUrlDecoder().decode(contextKey);
    var dac = Json.decodeValue(Buffer.buffer(dacJson), org.hpcclab.oaas.model.DataAccessContext.class);
    var clsName = dac.getCls(UUID.fromString(oid));
    return clsRepo.getAsync(clsName)
      .onItem().ifNull().failWith(NotFoundException::new)
      .flatMap(cls -> handleDataAccess(oid,key,cls,dac));
  }

  private Uni<Response> handleDataAccess(String oid,
                                         String key,
                                         OaasClass cls,
                                         org.hpcclab.oaas.model.DataAccessContext dac) {
    var provider = cls.getStateSpec().getDefaultProvider();
    var adapter = adapterMap.get(provider);
    if (adapter == null)
      return Uni.createFrom().item(Response.status(400).build());
    return adapter.get(new DataAccessRequest(oid,key, dac));
  }
}
