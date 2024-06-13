package org.hpcclab.oaas.sa.rest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.data.DataAccessRequest;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.sa.SaConfig;
import org.hpcclab.oaas.sa.adapter.DataRelayer;
import org.hpcclab.oaas.storage.S3Adapter;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@Path("/contents")
@ApplicationScoped
public class DataAccessResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessResource.class);

  private final ClassRepository clsRepo;
  private final S3Adapter s3Adapter;
  private final DataRelayer dataRelayer;
  private final boolean relay;

  public DataAccessResource(ClassRepository clsRepo, S3Adapter s3Adapter,
                            SaConfig config,
                            DataRelayer dataRelayer) {
    this.clsRepo = clsRepo;
    this.s3Adapter = s3Adapter;
    this.dataRelayer = dataRelayer;

    relay = config.relay();
  }

  @GET
  @Path("{oid}/{vid}/{key}")
  public Uni<Response> get(String oid,
                           String vid,
                           String key,
                           @RestQuery String contextKey) {
    if (contextKey==null) throw new StdOaasException("'contextKey' query param is required", 400);
    var dac = DataAccessContext.parse(contextKey);
    var clsName = dac.getCls();
    var cls = clsRepo.get(clsName);
    if (cls==null) throw StdOaasException.notFoundCls400(clsName);
    var url = s3Adapter.get(new DataAccessRequest(oid, vid, cls, key, dac));
    if (relay) {
      return dataRelayer.relay(url);
    }
    return Uni.createFrom().item(
      Response.temporaryRedirect(URI.create(url)).build()
    );


  }
}
