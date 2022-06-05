package org.hpcclab.oaas.storage.rest;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.data.DataAccessRequest;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.repository.impl.OaasClassRepository;
import org.hpcclab.oaas.storage.AdapterLoader;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/contents")
@ApplicationScoped
public class DataAccessResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessResource.class);

  @Inject
  OaasClassRepository clsRepo;
  @Inject
  AdapterLoader adapterLoader;

  @GET
  @Path("{oid}/{key}")
  public Uni<Response> get(String oid,
                           String key,
                           @RestQuery String contextKey) {
    // TODO protect contextKey with encryption and signature
    if (contextKey==null) throw new NoStackException("'contextKey' query param is required", 400);
    var dac = DataAccessContext.parse(contextKey);
    var clsName = dac.getCls();
    var cls =  clsRepo.get(clsName);
    if (cls == null) throw  NoStackException.notFoundCls400(clsName);
    return handleDataAccess(oid, key, cls, dac);
  }



  private Uni<Response> handleDataAccess(String oid,
                                         String key,
                                         OaasClass cls,
                                         DataAccessContext dac) {
    var adapter = adapterLoader.load(key, cls);
    return adapter.get(new DataAccessRequest(oid, cls, key, dac));
  }
}
