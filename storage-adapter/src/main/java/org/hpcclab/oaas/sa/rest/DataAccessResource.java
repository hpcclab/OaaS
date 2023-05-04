package org.hpcclab.oaas.sa.rest;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.data.DataAccessRequest;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.sa.AdapterLoader;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/contents")
@ApplicationScoped
public class DataAccessResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessResource.class);

  @Inject
  ClassRepository clsRepo;
  @Inject
  AdapterLoader adapterLoader;

  @GET
  @Path("{oid}/{vid}/{key}")
  public Uni<Response> get(String oid,
                           String vid,
                           String key,
                           @RestQuery String contextKey) {
    // TODO protect contextKey with encryption and signature
    if (contextKey==null) throw new NoStackException("'contextKey' query param is required", 400);
    var dac = DataAccessContext.parse(contextKey);
    var clsName = dac.getCls();
    var cls =  clsRepo.get(clsName);
    if (cls == null) throw  StdOaasException.notFoundCls400(clsName);
    var adapter = adapterLoader.load(key, cls);
    return adapter.get(new DataAccessRequest(oid, vid, cls, key, dac));
  }
}
