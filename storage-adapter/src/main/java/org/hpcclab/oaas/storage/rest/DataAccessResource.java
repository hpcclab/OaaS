package org.hpcclab.oaas.storage.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.DataAccessContext;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.storage.AdapterLoader;
import org.hpcclab.oaas.storage.DataAccessRequest;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.UUID;

@Path("/contents")
@ApplicationScoped
public class DataAccessResource {
  private static final Logger LOGGER = LoggerFactory.getLogger( DataAccessResource.class );

  @Inject
  OaasClassRepository clsRepo;
  @Inject
  AdapterLoader adapterLoader;



  @GET
  @Path("{oid}/{key}")
  public Uni<Response> get(String oid,
                        String key,
                        @RestQuery String contextKey) {
    var dacJson = Base64.getUrlDecoder().decode(contextKey);
    var dac = Json.decodeValue(Buffer.buffer(dacJson), DataAccessContext.class);
    var clsName = dac.getCls(UUID.fromString(oid));
    return clsRepo.getAsync(clsName)
      .onItem().ifNull().failWith(() -> NoStackException.notFoundCls400(clsName))
      .flatMap(cls -> handleDataAccess(oid,key,cls,dac));
  }

  private Uni<Response> handleDataAccess(String oid,
                                         String key,
                                         OaasClass cls,
                                         DataAccessContext dac) {
   var adapter = adapterLoader.load(key, cls);
    return adapter.get(new DataAccessRequest(oid,cls,key, dac));
  }
}
