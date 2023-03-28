package org.hpcclab.oaas.storage.rest;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.storage.AdapterLoader;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/allocate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataAllocateResource {
  @Inject
  AdapterLoader adapterLoader;
  @Inject
  ClassRepository clsRepo;

  @POST
  public Multi<DataAllocateResponse> allocate(List<DataAllocateRequest> requests) {
    return Multi.createFrom().iterable(requests)
      .onItem().transformToUniAndConcatenate(req ->
        adapterLoader.aggregatedAllocate(req)
          .map(map -> new DataAllocateResponse(req.getOid(), map)
          )
      );
  }


  @POST
  @Path("{oid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Map<String,String>> allocate(@RestPath String oid,
                                          @RestQuery String contextKey,
                                          List<String> keys) {
    var dac = DataAccessContext.parse(contextKey);
    var ksl = Lists.fixedSize.ofAll(keys)
      .collect(KeySpecification::new);
    var req = new DataAllocateRequest(oid,dac.getVid(),ksl,"s3", false);
    return adapterLoader.aggregatedAllocate(req);
  }

  @GET
  @Path("{oid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Map<String, String>> getAllocatedUrls(String oid,
                                                   @RestQuery String contextKey) {
    var dac = DataAccessContext.parse(contextKey);
    var clsName = dac.getCls();
    var cls =  clsRepo.get(clsName);
    if (cls == null) throw  StdOaasException.notFoundCls400(clsName);
    var req = new DataAllocateRequest(oid,
      dac.getVid(),
      cls.getStateSpec().getKeySpecs(),
      null,
      false);
    return adapterLoader.aggregatedAllocate(req);
  }
}
